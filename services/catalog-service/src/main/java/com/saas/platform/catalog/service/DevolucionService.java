package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.DevolucionDTOs;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Devoluciones de venta (por cualquier motivo) sobre órdenes CONFIRMADAS o COMPLETADAS.
 * En una sola transacción:
 *  1. Por cada línea devuelta: ENTRADA al inventario (movimiento ligado a la orden) y destino de la unidad:
 *       disponible -> vuelve a la venta · revision -> en reparación · baja -> se da de baja (daño/pérdida)
 *  2. Dinero: monto = valor de lo devuelto, prorrateando descuento e impuestos globales de la orden.
 *       a) primero reduce lo que el cliente aún debía (cuenta por cobrar);
 *       b) el excedente ya pagado -> NOTA CRÉDITO al cliente: reembolso o saldo a favor.
 *  3. Auditoría.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DevolucionService {

    public static final Map<String, String> MOTIVOS = Map.of(
        "garantia", "Garantía / defecto", "cambio", "Cambio de producto",
        "error_venta", "Error en la venta", "desistimiento", "Desistimiento del cliente", "otro", "Otro");
    private static final Set<String> DESTINOS = Set.of("disponible", "revision", "baja");

    private final OrdenRepository ordenRepo;
    private final DetalleOrdenRepository detalleRepo;
    private final PagoOrdenRepository pagoRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final UnidadRepository unidadRepo;
    private final BodegaRepository bodegaRepo;
    private final DevolucionVentaRepository devolucionRepo;
    private final DetalleDevolucionRepository detalleDevRepo;
    private final NotaCreditoClienteRepository notaClienteRepo;
    private final CuentaService cuentaService;
    private final BajaUnidadService bajaService;
    private final AuditoriaService auditoria;
    private final ContabilidadService contabilidad;
    private final AsientoContableRepository asientoRepo;
    @PersistenceContext private EntityManager em;

    @Transactional
    public DevolucionDTOs.Devolucion registrar(Long ordenId, DevolucionDTOs.Request req, Long orgId, Long usuarioId) {
        Orden orden = ordenRepo.findById(ordenId).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        if (orden.getEstado() != Orden.EstadoOrden.confirmada && orden.getEstado() != Orden.EstadoOrden.completada)
            throw new InvalidOperationException("Solo se registran devoluciones de órdenes confirmadas o completadas. "
                + "Si la orden aún está en borrador o en proceso, quita la unidad o reversa la orden.");
        if (req.getTipoMotivo() == null || !MOTIVOS.containsKey(req.getTipoMotivo()))
            throw new InvalidOperationException("Motivo inválido. Usa: " + String.join(", ", MOTIVOS.keySet()));
        if (req.getObservacion() == null || req.getObservacion().trim().length() < 10)
            throw new InvalidOperationException("Describe la devolución en la observación (mínimo 10 caracteres)");
        if (req.getItems() == null || req.getItems().isEmpty())
            throw new InvalidOperationException("Selecciona al menos una unidad a devolver");
        String obs = req.getObservacion().trim();

        // Prorrateo: lo que el cliente pagó realmente por cada línea (descuento e impuestos globales)
        List<DetalleOrden> lineas = detalleRepo.findByOrdenId(ordenId);
        BigDecimal sumaLineas = lineas.stream().map(DetalleOrden::getSubtotal).filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal factor = sumaLineas.signum() > 0
            ? orden.getTotal().divide(sumaLineas, 10, RoundingMode.HALF_UP) : BigDecimal.ONE;

        long previas = devolucionRepo.countByOrdenId(ordenId);
        DevolucionVenta dev = DevolucionVenta.builder()
            .organizacionId(orgId).orden(orden)
            .clienteId(orden.getCliente() != null ? orden.getCliente().getId() : null)
            .numero(orden.getNumeroOrden() + "-DEV" + (previas + 1))
            .tipoMotivo(req.getTipoMotivo()).observacion(obs).montoTotal(BigDecimal.ZERO)
            .usuarioId(usuarioId).build();
        String motivoMov = "Devolución de venta " + dev.getNumero() + " (" + MOTIVOS.get(req.getTipoMotivo()) + "): " + obs;

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal costoTotal = BigDecimal.ZERO, costoBaja = BigDecimal.ZERO;   // para el asiento contable
        List<Long> unidadesBaja = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        for (DevolucionDTOs.Item it : req.getItems()) {
            DetalleOrden d = lineas.stream().filter(l -> l.getId().equals(it.getDetalleId())).findFirst()
                .orElseThrow(() -> new InvalidOperationException("La línea " + it.getDetalleId() + " no pertenece a esta orden"));
            if (!vistos.add(d.getId())) throw new InvalidOperationException("La misma línea aparece dos veces en la devolución");
            int cant = it.getCantidad() == null ? 1 : it.getCantidad();
            int yaDevuelto = Optional.ofNullable(detalleDevRepo.cantidadDevuelta(d.getId())).orElse(0);
            int vendida = d.getCantidad() != null ? d.getCantidad() : 1;
            if (cant <= 0 || cant > vendida - yaDevuelto)
                throw new InvalidOperationException("De la unidad " + d.getUnidad().getCodigoUnico() + " se pueden devolver como máximo "
                    + (vendida - yaDevuelto) + " (vendidas " + vendida + ", ya devueltas " + yaDevuelto + ")");
            String destino = it.getDestino() == null ? "disponible" : it.getDestino();
            if (!DESTINOS.contains(destino)) throw new InvalidOperationException("Destino inválido: " + destino);

            Unidad u = d.getUnidad();
            Bodega bodega = resolverBodega(it.getBodegaId(), u.getId(), orgId);

            // 1) Entrada al inventario
            Inventario inv = inventarioRepo.findByBodegaIdAndUnidadId(bodega.getId(), u.getId())
                .orElseGet(() -> inventarioRepo.save(Inventario.builder().bodega(bodega).unidad(u).cantidad(0).build()));
            int anterior = inv.getCantidad() != null ? inv.getCantidad() : 0;
            inv.setCantidad(anterior + cant);
            inventarioRepo.save(inv);
            movimientoRepo.save(MovimientoInventario.builder()
                .inventario(inv).ordenId(ordenId).tipo(MovimientoInventario.TipoMovimiento.entrada)
                .cantidad(cant).cantidadAnterior(anterior).cantidadNueva(anterior + cant)
                .motivo(motivoMov).usuarioId(usuarioId).build());

            // Estado de la unidad según su condición al regresar
            u.setEstado(switch (destino) {
                case "revision" -> Unidad.EstadoUnidad.en_reparacion;
                case "baja" -> Unidad.EstadoUnidad.dañado;
                default -> Unidad.EstadoUnidad.disponible;
            });
            unidadRepo.save(u);
            if (destino.equals("baja")) {
                int existencias = inventarioRepo.findByUnidadId(u.getId()).stream()
                    .mapToInt(i -> i.getCantidad() != null ? i.getCantidad() : 0).sum();
                if (existencias > cant)
                    throw new InvalidOperationException("La unidad " + u.getCodigoUnico() + " es un lote con más existencias: "
                        + "usa el destino 'revisión' y luego un ajuste de inventario por la pieza dañada.");
                unidadesBaja.add(u.getId());
            }

            // Valor acreditado de la línea (precio efectivamente cobrado)
            BigDecimal monto = d.getSubtotal().divide(BigDecimal.valueOf(vendida), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(cant)).multiply(factor).setScale(2, RoundingMode.HALF_UP);
            total = total.add(monto);
            BigDecimal costo = (u.getPrecioCompra() != null ? u.getPrecioCompra() : BigDecimal.ZERO).multiply(BigDecimal.valueOf(cant));
            costoTotal = costoTotal.add(costo);
            if (destino.equals("baja")) costoBaja = costoBaja.add(costo);
            dev.getDetalles().add(DetalleDevolucion.builder().devolucion(dev).detalleOrdenId(d.getId())
                .unidadId(u.getId()).cantidad(cant).monto(monto).destino(destino).bodegaId(bodega.getId()).build());
        }

        // 2) Dinero: primero lo que el cliente debía; el excedente ya pagado -> nota crédito
        BigDecimal pagado = pagoRepo.findByOrdenIdOrderByFechaPagoDesc(ordenId).stream()
            .map(PagoOrden::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal devueltoTotal = orden.getMontoDevuelto().add(total);
        BigDecimal totalEfectivo = orden.getTotal().subtract(devueltoTotal);
        BigDecimal saldoAnterior = orden.getSaldoPendiente() != null ? orden.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal nuevoSaldo = totalEfectivo.subtract(pagado).max(BigDecimal.ZERO);
        BigDecimal aplicadoSaldo = saldoAnterior.subtract(nuevoSaldo).max(BigDecimal.ZERO).min(total);
        BigDecimal excedente = total.subtract(aplicadoSaldo);

        orden.setMontoDevuelto(devueltoTotal);
        orden.setSaldoPendiente(nuevoSaldo);
        if (nuevoSaldo.signum() == 0 && orden.getEstado() == Orden.EstadoOrden.confirmada)
            orden.setEstado(Orden.EstadoOrden.completada);
        ordenRepo.save(orden);
        cuentaService.sincronizarCxC(orden);

        dev.setMontoTotal(total);
        dev.setMontoAplicadoSaldo(aplicadoSaldo);
        dev.setMontoNotaCredito(excedente);
        if (excedente.signum() > 0) {
            String trat = req.getTratamiento();
            if (orden.getCliente() == null && "saldo_a_favor".equals(trat))
                throw new InvalidOperationException("La orden no tiene cliente: el excedente solo puede reembolsarse");
            if (!"reembolso".equals(trat) && !"saldo_a_favor".equals(trat))
                throw new InvalidOperationException("El cliente ya había pagado " + excedente
                    + " de lo devuelto: indica si se le reembolsa o queda como saldo a favor");
            if ("reembolso".equals(trat) && (req.getMetodoReembolso() == null || req.getMetodoReembolso().isBlank()))
                throw new InvalidOperationException("Indica el método del reembolso");
            dev.setTratamiento(trat);
            dev.setMetodoReembolso(req.getMetodoReembolso());
            dev.setReferenciaReembolso(req.getReferenciaReembolso());
        }
        dev = devolucionRepo.save(dev);

        if (excedente.signum() > 0) {
            boolean saldoFavor = "saldo_a_favor".equals(dev.getTratamiento());
            notaClienteRepo.save(NotaCreditoCliente.builder()
                .organizacionId(orgId).clienteId(dev.getClienteId()).devolucionId(dev.getId())
                .monto(excedente).saldoDisponible(saldoFavor ? excedente : BigDecimal.ZERO)
                .tratamiento(dev.getTratamiento()).estado(saldoFavor ? "disponible" : "reembolsada").build());
            auditoria.registrar("NOTA_CREDITO_CLIENTE", "devolucion", dev.getId(), Map.of(
                "monto", excedente, "tratamiento", dev.getTratamiento(),
                "metodoReembolso", String.valueOf(dev.getMetodoReembolso()), "referencia", String.valueOf(dev.getReferenciaReembolso())));
        }

        // Asiento contable de la devolución (partida doble)
        BigDecimal iva = orden.getTotal().signum() > 0 && orden.getImpuestos() != null
            ? total.multiply(orden.getImpuestos()).divide(orden.getTotal(), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        String tercero = orden.getCliente() != null
            ? orden.getCliente().getNombre() + (orden.getCliente().getDocumento() != null ? " (" + orden.getCliente().getDocumento() + ")" : "")
            : "Cliente ocasional";
        String cuentaExcedente = "saldo_a_favor".equals(dev.getTratamiento()) ? "contab.anticipos_clientes"
            : "efectivo".equals(dev.getMetodoReembolso()) ? "contab.caja" : "contab.bancos";
        String ref = "Devolución " + dev.getNumero() + " de la factura " + orden.getNumeroOrden();
        contabilidad.registrar(orgId, "devolucion_venta", dev.getNumero(), dev.getId(), ref + " — " + MOTIVOS.get(dev.getTipoMotivo()) + ": " + obs,
            List.of(
                ContabilidadService.Linea.debe("contab.devoluciones_ventas", total.subtract(iva), tercero, "Valor neto devuelto"),
                ContabilidadService.Linea.debe("contab.iva_por_pagar", iva, tercero, "IVA de lo devuelto"),
                ContabilidadService.Linea.haber("contab.clientes", aplicadoSaldo, tercero, "Disminuye la cuenta por cobrar"),
                ContabilidadService.Linea.haber(cuentaExcedente, excedente, tercero,
                    "saldo_a_favor".equals(dev.getTratamiento()) ? "Saldo a favor del cliente" : "Reembolso al cliente"),
                ContabilidadService.Linea.debe("contab.inventario", costoTotal, null, "Reingreso al inventario (costo)"),
                ContabilidadService.Linea.haber("contab.costo_ventas", costoTotal, null, "Reversa del costo de ventas"),
                ContabilidadService.Linea.debe("contab.perdida_inventario", costoBaja, null, "Unidades devueltas inservibles"),
                ContabilidadService.Linea.haber("contab.inventario", costoBaja, null, "Baja de unidades inservibles")),
            usuarioId);

        // Unidades que regresaron inservibles: se dan de baja (salida de inventario; la deuda con el proveedor se mantiene)
        for (Long uid : unidadesBaja)
            bajaService.darDeBaja(uid, "dano_perdida", "Devuelta inservible en " + dev.getNumero() + ": " + obs, orgId);

        // 3) Auditoría
        auditoria.registrar("DEVOLUCION_VENTA", "orden", ordenId, Map.of(
            "numero", dev.getNumero(), "motivo", MOTIVOS.get(dev.getTipoMotivo()), "observacion", obs,
            "unidades", dev.getDetalles().size(), "montoTotal", total,
            "aplicadoASaldo", aplicadoSaldo, "notaCredito", excedente));
        log.info("Devolución {} registrada: {} (saldo {}, nota crédito {})", dev.getNumero(), total, aplicadoSaldo, excedente);
        return toDTO(dev);
    }

    @Transactional(readOnly = true)
    public List<DevolucionDTOs.Devolucion> listar(Long ordenId, Long orgId) {
        Orden o = ordenRepo.findById(ordenId).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!o.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        return devolucionRepo.findByOrdenIdOrderByCreatedAtDesc(ordenId).stream().map(this::toDTO).toList();
    }


    // ── Módulo de devoluciones ───────────────────────────────────────────

    /** Todas las devoluciones de la organización (filtros opcionales por fecha y por factura o cliente). */
    @Transactional(readOnly = true)
    public List<DevolucionDTOs.Devolucion> listarTodas(Long orgId, java.time.LocalDate desde, java.time.LocalDate hasta, String q) {
        String filtro = q == null ? "" : q.trim().toLowerCase();
        return devolucionRepo.findTop300ByOrganizacionIdOrderByCreatedAtDesc(orgId).stream()
            .filter(d -> desde == null || !d.getCreatedAt().toLocalDate().isBefore(desde))
            .filter(d -> hasta == null || !d.getCreatedAt().toLocalDate().isAfter(hasta))
            .filter(d -> filtro.isEmpty() || d.getNumero().toLowerCase().contains(filtro)
                || d.getOrden().getNumeroOrden().toLowerCase().contains(filtro)
                || (d.getOrden().getCliente() != null && (d.getOrden().getCliente().getNombre().toLowerCase().contains(filtro)
                    || String.valueOf(d.getOrden().getCliente().getDocumento()).toLowerCase().contains(filtro))))
            .map(this::toDTO).toList();
    }

    /** Devoluciones paginadas; q busca por número de devolución, de factura, cliente o documento. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<DevolucionDTOs.Devolucion> paginado(Long orgId, java.time.LocalDate desde, java.time.LocalDate hasta,
                                                   String q, Integer page, Integer size) {
        final String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        org.springframework.data.jpa.domain.Specification<DevolucionVenta> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("organizacionId"), orgId));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.<java.time.LocalDateTime>get("createdAt"), desde.atStartOfDay()));
            if (hasta != null) ps.add(cb.lessThan(root.<java.time.LocalDateTime>get("createdAt"), hasta.plusDays(1).atStartOfDay()));
            if (like != null) {
                var o = root.join("orden", jakarta.persistence.criteria.JoinType.LEFT);
                var c = o.join("cliente", jakarta.persistence.criteria.JoinType.LEFT);
                ps.add(cb.or(cb.like(cb.lower(root.get("numero")), like), cb.like(cb.lower(o.get("numeroOrden")), like),
                    cb.like(cb.lower(c.get("nombre")), like), cb.like(cb.lower(c.get("documento")), like)));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return com.saas.platform.catalog.dto.PaginaDTO.de(devolucionRepo.findAll(spec, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id"))), this::toDTO);
    }

    /** Detalle con su asiento contable. */
    @Transactional(readOnly = true)
    public DevolucionDTOs.Devolucion detalle(Long id, Long orgId) {
        DevolucionVenta d = devolucionRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada"));
        if (!d.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Devolución no encontrada");
        DevolucionDTOs.Devolucion dto = toDTO(d);
        dto.setAsiento(asientoRepo.findFirstByTipoAndDocumentoId("devolucion_venta", id)
            .map(a -> a.getMovimientos().stream().map(m -> {
                java.util.Map<String, Object> x = new java.util.LinkedHashMap<>();
                x.put("cuenta", m.getCuentaCodigo()); x.put("nombre", m.getCuentaNombre());
                x.put("debe", m.getDebe()); x.put("haber", m.getHaber());
                x.put("tercero", m.getTercero()); x.put("detalle", m.getDetalle());
                return x;
            }).toList()).orElse(List.of()));
        return dto;
    }

    /** Facturas (órdenes confirmadas o completadas) por número o por nombre/documento del cliente. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<java.util.Map<String, Object>> buscarFacturas(String q, Long orgId) {
        if (q == null || q.trim().length() < 2) return List.of();
        List<Object[]> filas = em.createNativeQuery(
                "SELECT o.id, o.numero_orden, o.fecha_orden, c.nombre, c.documento, o.total, o.monto_devuelto, CAST(o.estado AS varchar) " +
                "FROM ordenes o LEFT JOIN clientes c ON c.id = o.cliente_id " +
                "WHERE o.organizacion_id = :org AND CAST(o.estado AS varchar) IN ('confirmada', 'completada') " +
                "AND (LOWER(o.numero_orden) LIKE :q OR LOWER(COALESCE(c.nombre, '')) LIKE :q OR LOWER(COALESCE(c.documento, '')) LIKE :q) " +
                "ORDER BY o.fecha_orden DESC LIMIT 20")
            .setParameter("org", orgId).setParameter("q", "%" + q.trim().toLowerCase() + "%").getResultList();
        String[] cols = {"id", "numero", "fecha", "cliente", "documento", "total", "montoDevuelto", "estado"};
        List<java.util.Map<String, Object>> out = new ArrayList<>();
        for (Object[] f : filas) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            for (int i = 0; i < cols.length; i++) m.put(cols[i], f[i] != null ? String.valueOf(f[i]) : null);
            out.add(m);
        }
        return out;
    }

    public boolean tieneDevoluciones(Long ordenId) { return devolucionRepo.countByOrdenId(ordenId) > 0; }

    /** Saldo a favor disponible de un cliente (notas crédito con tratamiento 'saldo_a_favor'). */
    @Transactional(readOnly = true)
    public BigDecimal saldoAFavor(Long clienteId) {
        if (clienteId == null) return BigDecimal.ZERO;
        return notaClienteRepo.findByClienteIdAndEstadoOrderByCreatedAtAsc(clienteId, "disponible").stream()
            .map(NotaCreditoCliente::getSaldoDisponible).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Usa saldo a favor como forma de pago: consume las notas más antiguas primero. */
    @Transactional
    public void consumirSaldoAFavor(Long clienteId, BigDecimal monto) {
        if (saldoAFavor(clienteId).compareTo(monto) < 0)
            throw new InvalidOperationException("El cliente solo tiene " + saldoAFavor(clienteId) + " de saldo a favor");
        BigDecimal restante = monto;
        for (NotaCreditoCliente n : notaClienteRepo.findByClienteIdAndEstadoOrderByCreatedAtAsc(clienteId, "disponible")) {
            if (restante.signum() <= 0) break;
            BigDecimal usar = n.getSaldoDisponible().min(restante);
            n.setSaldoDisponible(n.getSaldoDisponible().subtract(usar));
            if (n.getSaldoDisponible().signum() == 0) n.setEstado("aplicada");
            notaClienteRepo.save(n);
            restante = restante.subtract(usar);
        }
    }

    private Bodega resolverBodega(Long bodegaId, Long unidadId, Long orgId) {
        Long id = bodegaId;
        if (id == null) {   // por defecto: la bodega de donde salió
            id = inventarioRepo.findByUnidadId(unidadId).stream().map(i -> i.getBodega().getId()).findFirst()
                .orElseThrow(() -> new InvalidOperationException("Indica la bodega a la que reingresa la unidad"));
        }
        long deOrg = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM bodegas b JOIN sedes s ON s.id = b.sede_id " +
                "JOIN ciudades_organizaciones co ON co.id = s.ciudad_organizacion_id " +
                "WHERE b.id = :b AND co.organizacion_id = :o AND b.activo = TRUE")
            .setParameter("b", id).setParameter("o", orgId).getSingleResult()).longValue();
        if (deOrg == 0) throw new InvalidOperationException("La bodega de reingreso no existe o está inactiva");
        return bodegaRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada"));
    }

    private DevolucionDTOs.Devolucion toDTO(DevolucionVenta d) {
        List<DevolucionDTOs.Linea> lineas = d.getDetalles().stream().map(x -> {
            Unidad u = unidadRepo.findById(x.getUnidadId()).orElse(null);
            return DevolucionDTOs.Linea.builder().detalleOrdenId(x.getDetalleOrdenId())
                .unidadCodigo(u != null ? u.getCodigoUnico() : null)
                .productoNombre(u != null && u.getProducto() != null ? u.getProducto().getNombre() : null)
                .cantidad(x.getCantidad()).monto(x.getMonto()).destino(x.getDestino()).build();
        }).toList();
        Orden o = d.getOrden();
        return DevolucionDTOs.Devolucion.builder()
            .id(d.getId()).numero(d.getNumero())
            .ordenId(o != null ? o.getId() : null).ordenNumero(o != null ? o.getNumeroOrden() : null)
            .clienteNombre(o != null && o.getCliente() != null ? o.getCliente().getNombre() : null).tipoMotivo(d.getTipoMotivo()).observacion(d.getObservacion())
            .montoTotal(d.getMontoTotal()).montoAplicadoSaldo(d.getMontoAplicadoSaldo()).montoNotaCredito(d.getMontoNotaCredito())
            .tratamiento(d.getTratamiento()).metodoReembolso(d.getMetodoReembolso()).referenciaReembolso(d.getReferenciaReembolso())
            .createdAt(d.getCreatedAt()).lineas(lineas).build();
    }
}
