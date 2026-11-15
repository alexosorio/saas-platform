package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.BusinessException;
import com.saas.platform.catalog.exception.InsufficientStockException;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class OrdenService {

    private final OrdenRepository repo;
    private final DetalleOrdenRepository detalleRepo;
    private final PagoOrdenRepository pagoRepo;
    private final ClienteRepository clienteRepo;
    private final UnidadRepository unidadRepo;
    private final InventarioRepository inventarioRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final CuentaService cuentaService;
    private final BodegaAccesoService accesoBodegas;
    private final AuditoriaService auditoria;
    private final DevolucionService devolucionService;

    @Transactional(readOnly = true)
    public List<OrdenDTO> getAll(Long orgId) {
        return repo.findByOrganizacionIdOrderByCreatedAtDesc(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Órdenes paginadas (más recientes primero); q busca por número de orden, cliente o documento. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<OrdenDTO> paginado(Long orgId, String estado, String q, Integer page, Integer size) {
        Orden.EstadoOrden est = null;
        if (estado != null && !estado.isBlank()) {
            try { est = Orden.EstadoOrden.valueOf(estado); }
            catch (IllegalArgumentException e) { throw new InvalidOperationException("Estado de orden no válido: " + estado); }
        }
        final Orden.EstadoOrden filtroEstado = est;
        final String like = patron(q);
        org.springframework.data.jpa.domain.Specification<Orden> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("organizacionId"), orgId));
            if (filtroEstado != null) ps.add(cb.equal(root.get("estado"), filtroEstado));
            if (like != null) {
                var c = root.join("cliente", jakarta.persistence.criteria.JoinType.LEFT);
                ps.add(cb.or(cb.like(cb.lower(root.get("numeroOrden")), like),
                             cb.like(cb.lower(c.get("nombre")), like), cb.like(cb.lower(c.get("documento")), like)));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return com.saas.platform.catalog.dto.PaginaDTO.de(repo.findAll(spec, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id"))), this::toDTO);
    }

    private static String patron(String q) { return q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%"; }

    @Transactional(readOnly = true)
    public List<OrdenDTO> getPorEstado(Long orgId, String estado) {
        return repo.findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(orgId, Orden.EstadoOrden.valueOf(estado))
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdenDTO getById(Long id, Long orgId) {
        Orden o = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!o.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        return toDTO(o);
    }

    @Transactional
    public OrdenDTO create(CreateOrdenRequest req, Long orgId, Long usuarioId) {
        Cliente cliente = clienteRepo.findById(req.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        accesoBodegas.validarSedeOrden(req.getSedeId(), orgId);   // otra sede solo con permiso

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemOrdenRequest item : req.getItems()) {
            BigDecimal sub = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            if (item.getDescuentoLinea() != null) sub = sub.subtract(item.getDescuentoLinea());
            subtotal = subtotal.add(sub);
        }
        BigDecimal descuento = req.getDescuento() != null ? req.getDescuento() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(descuento);

        Orden orden = Orden.builder()
            .organizacionId(orgId).sedeId(req.getSedeId()).cliente(cliente).usuarioId(usuarioId)
            .numeroOrden(generarNumero()).fechaOrden(LocalDate.now())
            .subtotal(subtotal).descuento(descuento).impuestos(BigDecimal.ZERO)
            .total(total).saldoPendiente(total).estado(Orden.EstadoOrden.borrador)
            .notas(req.getNotas()).build();
        Orden saved = repo.save(orden);

        for (ItemOrdenRequest item : req.getItems()) {
            Unidad unidad = unidadRepo.findById(item.getUnidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada: " + item.getUnidadId()));
            accesoBodegas.validarVenta(unidad);   // no vender desde otra bodega sin permiso
            BigDecimal sub = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            if (item.getDescuentoLinea() != null) sub = sub.subtract(item.getDescuentoLinea());
            detalleRepo.save(DetalleOrden.builder()
                .orden(saved).unidad(unidad).cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .descuentoLinea(item.getDescuentoLinea() != null ? item.getDescuentoLinea() : BigDecimal.ZERO)
                .subtotal(sub).build());
        }
        log.info("Orden creada: {}", orden.getNumeroOrden());
        auditoria.registrar("ORDEN_CREADA", "orden", saved.getId(), java.util.Map.of("numero", String.valueOf(saved.getNumeroOrden()), "total", saved.getTotal(), "items", req.getItems().size(), "sede", req.getSedeId()));
        return getById(saved.getId(), orgId);
    }

    /** Pago en proceso: RESERVA (bloquea) el stock sin sacarlo. La orden queda en_proceso y es reversible. */
    @Transactional
    public OrdenDTO reservar(Long id, Long orgId, Long usuarioId) {
        Orden orden = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        if (orden.getEstado() != Orden.EstadoOrden.borrador)
            throw new InvalidOperationException("Solo se reservan ordenes en borrador");

        for (DetalleOrden detalle : orden.getDetalles()) {
            List<Inventario> invs = accesoBodegas.inventariosPreferidos(detalle.getUnidad().getId(), orden.getSedeId());
            int disponible = invs.stream().mapToInt(Inventario::getDisponible).sum();
            if (disponible < detalle.getCantidad())
                throw new InsufficientStockException("Stock disponible insuficiente para unidad: " + detalle.getUnidad().getCodigoUnico());

            int restante = detalle.getCantidad();
            for (Inventario inv : invs) {
                if (restante <= 0) break;
                int libre = inv.getDisponible();
                if (libre <= 0) continue;
                int res = Math.min(restante, libre);
                inv.setCantidadReservada((inv.getCantidadReservada() == null ? 0 : inv.getCantidadReservada()) + res);
                inventarioRepo.save(inv);
                restante -= res;
            }
            Unidad u = detalle.getUnidad();
            u.setEstado(Unidad.EstadoUnidad.reservado);
            unidadRepo.save(u);
        }
        orden.setEstado(Orden.EstadoOrden.en_proceso);
        log.info("Orden reservada (pago en proceso): {}", orden.getNumeroOrden());
        auditoria.registrar("ORDEN_RESERVADA", "orden", orden.getId(), java.util.Map.of("numero", String.valueOf(orden.getNumeroOrden()), "total", orden.getTotal()));
        return toDTO(repo.save(orden));
    }

    /** Confirma la orden: si venía reservada (en_proceso) COMPROMETE la reserva; si es borrador, descuenta directo. */
    @Transactional
    public OrdenDTO confirmar(Long id, Long orgId, Long usuarioId) {
        Orden orden = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        boolean reservada = orden.getEstado() == Orden.EstadoOrden.en_proceso;
        if (orden.getEstado() != Orden.EstadoOrden.borrador && !reservada)
            throw new InvalidOperationException("Solo se confirman ordenes en borrador o en proceso");

        for (DetalleOrden detalle : orden.getDetalles()) {
            List<Inventario> invs = accesoBodegas.inventariosPreferidos(detalle.getUnidad().getId(), orden.getSedeId());
            int base = reservada
                ? invs.stream().mapToInt(i -> i.getCantidadReservada() == null ? 0 : i.getCantidadReservada()).sum()
                : invs.stream().mapToInt(Inventario::getDisponible).sum();
            if (base < detalle.getCantidad())
                throw new InsufficientStockException("Stock insuficiente para unidad: " + detalle.getUnidad().getCodigoUnico());

            int restante = detalle.getCantidad();
            for (Inventario inv : invs) {
                if (restante <= 0) break;
                int tomar = reservada
                    ? Math.min(restante, inv.getCantidadReservada() == null ? 0 : inv.getCantidadReservada())
                    : Math.min(restante, inv.getDisponible());
                if (tomar <= 0) continue;
                int anterior = inv.getCantidad();
                inv.setCantidad(anterior - tomar);
                if (reservada) inv.setCantidadReservada(inv.getCantidadReservada() - tomar); // consume la reserva
                inventarioRepo.save(inv);
                movimientoRepo.save(MovimientoInventario.builder()
                    .inventario(inv).ordenId(orden.getId())
                    .tipo(MovimientoInventario.TipoMovimiento.salida)
                    .cantidad(tomar).cantidadAnterior(anterior).cantidadNueva(inv.getCantidad())
                    .motivo("Venta - Orden " + orden.getNumeroOrden()).usuarioId(usuarioId).build());
                restante -= tomar;
            }
            Unidad u = detalle.getUnidad();
            u.setEstado(Unidad.EstadoUnidad.vendido);
            unidadRepo.save(u);
        }
        orden.setEstado(Orden.EstadoOrden.confirmada);
        log.info("Orden confirmada: {}", orden.getNumeroOrden());
        Orden savedC = repo.save(orden);
        cuentaService.sincronizarCxC(savedC);
        auditoria.registrar("ORDEN_CONFIRMADA", "orden", orden.getId(), java.util.Map.of("numero", String.valueOf(orden.getNumeroOrden()), "total", orden.getTotal()));
        return toDTO(savedC);
    }

    /** Reversa una orden con pago en proceso: LIBERA la reserva y la vuelve a borrador. */
    @Transactional
    public OrdenDTO reversar(Long id, Long orgId, Long usuarioId) {
        Orden orden = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        if (orden.getEstado() != Orden.EstadoOrden.en_proceso)
            throw new InvalidOperationException("Solo se puede reversar una orden con pago en proceso");

        for (DetalleOrden detalle : orden.getDetalles()) {
            List<Inventario> invs = accesoBodegas.inventariosPreferidos(detalle.getUnidad().getId(), orden.getSedeId());
            int restante = detalle.getCantidad();
            for (Inventario inv : invs) {
                if (restante <= 0) break;
                int res = inv.getCantidadReservada() == null ? 0 : inv.getCantidadReservada();
                int liberar = Math.min(restante, res);
                if (liberar <= 0) continue;
                inv.setCantidadReservada(res - liberar);
                inventarioRepo.save(inv);
                restante -= liberar;
            }
            Unidad u = detalle.getUnidad();
            u.setEstado(Unidad.EstadoUnidad.disponible);
            unidadRepo.save(u);
        }
        orden.setEstado(Orden.EstadoOrden.borrador);
        log.info("Orden reversada (reserva liberada): {}", orden.getNumeroOrden());
        auditoria.registrar("ORDEN_REVERSADA", "orden", orden.getId(), java.util.Map.of("numero", String.valueOf(orden.getNumeroOrden()), "total", orden.getTotal()));
        return toDTO(repo.save(orden));
    }

    @Transactional
    public OrdenDTO actualizarEstado(Long id, String estado, Long orgId) {
        Orden orden = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        orden.setEstado(Orden.EstadoOrden.valueOf(estado));
        return toDTO(repo.save(orden));
    }

    @Transactional
    public OrdenDTO cancelar(Long id, String motivo, Long orgId, Long usuarioId) {
        Orden orden = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        if (devolucionService.tieneDevoluciones(orden.getId()))
            throw new InvalidOperationException("La orden tiene devoluciones registradas: no se puede cancelar. "
                + "Devuelve las unidades restantes con 'Registrar devolución' para que el inventario y el dinero cuadren.");
        if (orden.getEstado() == Orden.EstadoOrden.completada)
            throw new InvalidOperationException("No se puede cancelar una orden completada");

        if (orden.getEstado() == Orden.EstadoOrden.en_proceso) {
            // Pago en proceso: solo LIBERA la reserva (nunca se descontó el stock).
            for (DetalleOrden detalle : orden.getDetalles()) {
                Unidad u = detalle.getUnidad();
                u.setEstado(Unidad.EstadoUnidad.disponible);
                unidadRepo.save(u);
                int restante = detalle.getCantidad();
                for (Inventario inv : inventarioRepo.findByUnidadId(u.getId())) {
                    if (restante <= 0) break;
                    int res = inv.getCantidadReservada() == null ? 0 : inv.getCantidadReservada();
                    int liberar = Math.min(restante, res);
                    if (liberar <= 0) continue;
                    inv.setCantidadReservada(res - liberar);
                    inventarioRepo.save(inv);
                    restante -= liberar;
                }
            }
        } else if (orden.getEstado() == Orden.EstadoOrden.confirmada) {
            // Confirmada: el stock ya salió, hay que REINTEGRARLO.
            for (DetalleOrden detalle : orden.getDetalles()) {
                Unidad u = detalle.getUnidad();
                u.setEstado(Unidad.EstadoUnidad.disponible);
                unidadRepo.save(u);
                List<Inventario> invs = inventarioRepo.findByUnidadId(u.getId());
                if (!invs.isEmpty()) {
                    Inventario inv = invs.get(0);
                    int anterior = inv.getCantidad();
                    inv.setCantidad(anterior + detalle.getCantidad());
                    inventarioRepo.save(inv);
                    movimientoRepo.save(MovimientoInventario.builder()
                        .inventario(inv).ordenId(orden.getId())
                        .tipo(MovimientoInventario.TipoMovimiento.entrada)
                        .cantidad(detalle.getCantidad()).cantidadAnterior(anterior).cantidadNueva(inv.getCantidad())
                        .motivo("Cancelacion orden: " + motivo).usuarioId(usuarioId).build());
                }
            }
        }
        orden.setEstado(Orden.EstadoOrden.cancelada);
        orden.setNotas((orden.getNotas() != null ? orden.getNotas() + " | " : "") + "CANCELACION: " + motivo);
        log.info("Orden cancelada: {}", orden.getNumeroOrden());
        auditoria.registrar("ORDEN_CANCELADA", "orden", orden.getId(), java.util.Map.of("numero", String.valueOf(orden.getNumeroOrden()), "motivo", String.valueOf(motivo)));
        return toDTO(repo.save(orden));
    }

    @Transactional
    public PagoOrdenDTO registrarPago(Long ordenId, RegistrarPagoRequest req, Long orgId, Long usuarioId) {
        Orden orden = repo.findById(ordenId).orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        if (!orden.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Orden no encontrada");
        if (orden.getEstado() == Orden.EstadoOrden.cancelada)
            throw new InvalidOperationException("No se puede pagar una orden cancelada");
        if (req.getMonto().compareTo(orden.getSaldoPendiente()) > 0)
            throw new BusinessException("El monto excede el saldo pendiente: " + orden.getSaldoPendiente());

        // Pago con saldo a favor del cliente (notas crédito de devoluciones anteriores)
        if ("nota_credito".equals(req.getMetodo())) {
            if (orden.getCliente() == null) throw new InvalidOperationException("La orden no tiene cliente: no hay saldo a favor que usar");
            devolucionService.consumirSaldoAFavor(orden.getCliente().getId(), req.getMonto());
        }

        PagoOrden pago = PagoOrden.builder()
            .orden(orden).monto(req.getMonto())
            .metodo(PagoOrden.MetodoPago.valueOf(req.getMetodo()))
            .referencia(req.getReferencia()).observaciones(req.getObservaciones())
            .banco(req.getBanco()).fechaTransaccion(req.getFechaTransaccion())
            .comprobanteUrl(req.getComprobanteUrl()).ultimosDigitos(req.getUltimosDigitos())
            .usuarioRegistro(usuarioId).build();
        PagoOrden saved = pagoRepo.save(pago);

        BigDecimal nuevoSaldo = orden.getSaldoPendiente().subtract(req.getMonto());
        orden.setSaldoPendiente(nuevoSaldo);
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0 && orden.getEstado() == Orden.EstadoOrden.confirmada)
            orden.setEstado(Orden.EstadoOrden.completada);
        repo.save(orden);
        cuentaService.sincronizarCxC(orden);
        log.info("Pago registrado orden={} monto={}", orden.getNumeroOrden(), req.getMonto());
        auditoria.registrar("PAGO_ORDEN_REGISTRADO", "orden", orden.getId(), java.util.Map.of("pagoId", saved.getId(), "monto", String.valueOf(req.getMonto()), "metodo", String.valueOf(req.getMetodo()), "referencia", String.valueOf(req.getReferencia())));
        return toPagoDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PagoOrdenDTO> getPagos(Long ordenId) {
        return pagoRepo.findByOrdenIdOrderByFechaPagoDesc(ordenId).stream().map(this::toPagoDTO).collect(Collectors.toList());
    }

    private String generarNumero() {
        return "ORD-" + LocalDate.now().toString().replace("-", "") + "-" +
            UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private OrdenDTO toDTO(Orden o) {
        List<DetalleOrdenDTO> detalles = o.getDetalles() != null ?
            o.getDetalles().stream().map(this::toDetalleDTO).collect(Collectors.toList()) : List.of();
        return OrdenDTO.builder()
            .id(o.getId()).numeroOrden(o.getNumeroOrden()).organizacionId(o.getOrganizacionId())
            .sedeId(o.getSedeId()).sedeNombre(o.getSedeId() != null ? accesoBodegas.nombreSede(o.getSedeId()) : null)
            .clienteId(o.getCliente() != null ? o.getCliente().getId() : null)
            .clienteNombre(o.getCliente() != null ? o.getCliente().getNombre() : null)
            .usuarioId(o.getUsuarioId()).fechaOrden(o.getFechaOrden())
            .subtotal(o.getSubtotal()).descuento(o.getDescuento()).impuestos(o.getImpuestos())
            .total(o.getTotal()).saldoPendiente(o.getSaldoPendiente()).montoDevuelto(o.getMontoDevuelto())
            .estado(o.getEstado() != null ? o.getEstado().name() : null)
            .notas(o.getNotas()).detalles(detalles).createdAt(o.getCreatedAt()).build();
    }

    private String colorDe(Unidad u) {
        if (u == null || u.getValoresCaracteristicas() == null) return null;
        return u.getValoresCaracteristicas().stream()
            .filter(v -> v.getCaracteristica() != null && "color".equalsIgnoreCase(v.getCaracteristica().getCodigo()))
            .map(v -> ResumenUnidadesService.normalizarColor(v.getValor()))
            .findFirst().orElse(null);
    }

    private DetalleOrdenDTO toDetalleDTO(DetalleOrden d) {
        return DetalleOrdenDTO.builder()
            .id(d.getId())
            .unidadId(d.getUnidad() != null ? d.getUnidad().getId() : null)
            .unidadCodigo(d.getUnidad() != null ? d.getUnidad().getCodigoUnico() : null)
            .productoNombre(d.getUnidad() != null && d.getUnidad().getProducto() != null ? d.getUnidad().getProducto().getNombre() : null)
            .color(colorDe(d.getUnidad()))
            .cantidad(d.getCantidad()).precioUnitario(d.getPrecioUnitario())
            .descuentoLinea(d.getDescuentoLinea()).subtotal(d.getSubtotal()).build();
    }

    private PagoOrdenDTO toPagoDTO(PagoOrden p) {
        return PagoOrdenDTO.builder()
            .id(p.getId()).ordenId(p.getOrden() != null ? p.getOrden().getId() : null)
            .monto(p.getMonto()).metodo(p.getMetodo() != null ? p.getMetodo().name() : null)
            .referencia(p.getReferencia()).observaciones(p.getObservaciones())
            .banco(p.getBanco()).fechaTransaccion(p.getFechaTransaccion())
            .comprobanteUrl(p.getComprobanteUrl()).ultimosDigitos(p.getUltimosDigitos())
            .fechaPago(p.getFechaPago()).usuarioRegistro(p.getUsuarioRegistro()).build();
    }
}
