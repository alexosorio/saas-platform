package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.BusinessException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class CuentaService {

    private final CuentaPorPagarRepository cxpRepo;
    private final PagoProveedorRepository pagoProvRepo;
    private final CuentaPorCobrarRepository cxcRepo;
    private final ParametrosLector parametros;
    private final AuditoriaService auditoria;
    private final NotaCuentaPagarRepository notaRepo;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    @org.springframework.beans.factory.annotation.Value("${app.cuentas.plazo-pagar-dias:30}")
    private int plazoPagarDias;
    @org.springframework.beans.factory.annotation.Value("${app.cuentas.plazo-cobrar-dias:30}")
    private int plazoCobrarDias;

    // ── Cuentas por pagar ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CuentaPorPagarDTO> listarPorPagar(Long orgId, FiltroCuentas f) {
        return conNotas(cxpRepo.findAll(filtro(orgId, f, "proveedor", f.getProveedorId()),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            .stream().map(this::toCxpDTO).collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<PagoProveedorDTO> pagosDeCuenta(Long cuentaId, Long orgId) {
        resolverCxp(cuentaId, orgId);
        return pagoProvRepo.findByCuentaPorPagarIdOrderByCreatedAtDesc(cuentaId)
            .stream().map(this::toPagoProvDTO).collect(Collectors.toList());
    }

    @Transactional
    public PagoProveedorDTO registrarPagoProveedor(Long cuentaId, Long orgId, Long usuarioId, RegistrarPagoProveedorRequest req) {
        CuentaPorPagar c = resolverCxp(cuentaId, orgId);
        if (c.getSaldo() == null || c.getSaldo().signum() <= 0)
            throw new BusinessException("La cuenta no tiene saldo pendiente (estado: " + c.getEstado() + ")");
        BigDecimal monto = req.getMonto();
        if (monto == null || monto.signum() <= 0) throw new BusinessException("Monto inválido");
        if (monto.compareTo(c.getSaldo()) > 0) throw new BusinessException("El monto supera el saldo pendiente");

        PagoProveedor p = PagoProveedor.builder()
            .cuentaPorPagar(c).monto(monto)
            .metodo(req.getMetodo()).banco(req.getBanco()).comprobanteUrl(req.getComprobanteUrl())
            .ultimosDigitos(req.getUltimosDigitos()).fechaTransaccion(req.getFechaTransaccion())
            .referencia(req.getReferencia()).observaciones(req.getObservaciones())
            .usuarioRegistro(usuarioId).build();
        pagoProvRepo.save(p);

        c.setSaldo(c.getSaldo().subtract(monto));
        c.setEstado(c.getSaldo().signum() <= 0 ? "pagada" : "parcial");
        cxpRepo.save(c);
        log.info("Pago a proveedor registrado. Cuenta {} saldo {}", c.getId(), c.getSaldo());
        auditoria.registrar("PAGO_PROVEEDOR_REGISTRADO", "cuenta_por_pagar", c.getId(), java.util.Map.of("monto", monto, "metodo", String.valueOf(req.getMetodo()), "referencia", String.valueOf(req.getReferencia()), "saldoRestante", c.getSaldo()));
        return toPagoProvDTO(p);
    }


    /**
     * Nota CRÉDITO sobre una cuenta por pagar (p. ej. por baja de la unidad por error de registro o devolución).
     * Reduce el saldo; si ya se había pagado más de lo que queda, el saldo queda negativo = saldo a favor.
     */
    @Transactional
    public NotaCuentaPagar aplicarNotaCredito(CuentaPorPagar c, BigDecimal monto, String motivo, Long unidadId, Long usuarioId) {
        if (monto == null || monto.signum() <= 0) return null;
        NotaCuentaPagar nota = notaRepo.save(NotaCuentaPagar.builder()
            .cuentaPorPagar(c).unidadId(unidadId).tipo("credito").monto(monto).motivo(motivo).usuarioId(usuarioId).build());
        BigDecimal nuevo = c.getSaldo().subtract(monto);
        c.setSaldo(nuevo);
        boolean huboPagos = !pagoProvRepo.findByCuentaPorPagarIdOrderByCreatedAtDesc(c.getId()).isEmpty();
        c.setEstado(nuevo.signum() < 0 ? "saldo_a_favor" : nuevo.signum() == 0 ? (huboPagos ? "pagada" : "anulada") : "parcial");
        cxpRepo.save(c);
        auditoria.registrar("NOTA_CREDITO_CXP", "cuenta_por_pagar", c.getId(), java.util.Map.of(
            "monto", monto, "saldoResultante", nuevo, "estado", c.getEstado(), "motivo", motivo));
        return nota;
    }

    /** Hook: crea la cuenta por pagar al ingresar una unidad al inventario. */
    @Transactional
    public void crearCxPParaUnidad(Unidad unidad) {
        BigDecimal costo = unidad.getPrecioCompra();
        if (costo == null || costo.signum() <= 0) return;   // sin costo -> no genera CxP
        CuentaPorPagar c = CuentaPorPagar.builder()
            .organizacionId(unidad.getProducto() != null ? unidad.getProducto().getOrganizacionId() : null)
            .unidad(unidad).proveedor(unidad.getProveedor())
            .monto(costo).saldo(costo).estado("pendiente")
            .fechaVencimiento(java.time.LocalDate.now().plusDays(parametros.entero("cuentas.plazo_pagar_dias",
                unidad.getProducto() != null ? unidad.getProducto().getOrganizacionId() : null, plazoPagarDias)))
            .descripcion("Ingreso de unidad " + unidad.getCodigoUnico()
                + (unidad.getProducto() != null ? " (" + unidad.getProducto().getNombre() + ")" : ""))
            .build();
        cxpRepo.save(c);
    }

    /** Cuentas por pagar paginadas; el resumen trae el saldo pendiente y el total de TODO el filtro. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<CuentaPorPagarDTO> paginarPorPagar(Long orgId, FiltroCuentas f, Integer page, Integer size) {
        org.springframework.data.jpa.domain.Specification<CuentaPorPagar> spec = filtro(orgId, f, "proveedor", f.getProveedorId());
        var pagina = cxpRepo.findAll(spec, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id")));
        return com.saas.platform.catalog.dto.PaginaDTO.de(pagina, conNotas(pagina.getContent().stream().map(this::toCxpDTO).collect(Collectors.toList())))
            .conResumen(resumenSaldos(CuentaPorPagar.class, spec));
    }

    /** Cuentas por cobrar paginadas; el resumen trae el saldo pendiente y el total de TODO el filtro. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<CuentaPorCobrarDTO> paginarPorCobrar(Long orgId, FiltroCuentas f, Integer page, Integer size) {
        org.springframework.data.jpa.domain.Specification<CuentaPorCobrar> spec = filtro(orgId, f, "cliente", f.getClienteId());
        var pagina = cxcRepo.findAll(spec, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id")));
        return com.saas.platform.catalog.dto.PaginaDTO.de(pagina, this::toCxcDTO).conResumen(resumenSaldos(CuentaPorCobrar.class, spec));
    }

    /** Suma de saldo y de monto sobre todo el filtro (no solo la página visible). */
    private <E> java.util.Map<String, Object> resumenSaldos(Class<E> tipo, org.springframework.data.jpa.domain.Specification<E> spec) {
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(Object[].class);
        var root = q.from(tipo);
        q.multiselect(cb.coalesce(cb.sum(root.<BigDecimal>get("saldo")), BigDecimal.ZERO),
                      cb.coalesce(cb.sum(root.<BigDecimal>get("monto")), BigDecimal.ZERO));
        jakarta.persistence.criteria.Predicate p = spec.toPredicate(root, q, cb);
        if (p != null) q.where(p);
        Object[] r = em.createQuery(q).getSingleResult();
        return java.util.Map.of("saldoPendiente", r[0], "montoTotal", r[1]);
    }

    private List<CuentaPorPagarDTO> conNotas(List<CuentaPorPagarDTO> l) {
        if (!l.isEmpty()) {
            var notas = notaRepo.findByCuentaPorPagarIdInOrderByCreatedAtAsc(l.stream().map(CuentaPorPagarDTO::getId).toList());
            l.forEach(d -> d.setNotas(notas.stream().filter(n -> n.getCuentaPorPagar().getId().equals(d.getId()))
                .map(n -> java.util.Map.<String, Object>of("monto", n.getMonto(), "motivo", n.getMotivo(),
                    "fecha", String.valueOf(n.getCreatedAt()))).toList()));
        }
        return l;
    }

    // ── Cuentas por cobrar ───────────────────────────────────
    @Transactional(readOnly = true)
    public List<CuentaPorCobrarDTO> listarPorCobrar(Long orgId, FiltroCuentas f) {
        return cxcRepo.findAll(filtro(orgId, f, "cliente", f.getClienteId()),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            .stream().map(this::toCxcDTO).collect(Collectors.toList());
    }

    /** Hook: crea/actualiza la cuenta por cobrar según el saldo pendiente de la orden. */
    @Transactional
    public void sincronizarCxC(Orden orden) {
        BigDecimal saldo = orden.getSaldoPendiente() != null ? orden.getSaldoPendiente() : BigDecimal.ZERO;
        // Total efectivo: descuenta lo acreditado por devoluciones
        BigDecimal totalEf = orden.getTotal().subtract(orden.getMontoDevuelto() != null ? orden.getMontoDevuelto() : BigDecimal.ZERO);
        CuentaPorCobrar c = cxcRepo.findByOrdenId(orden.getId()).orElse(null);
        if (c == null) {
            if (saldo.signum() <= 0) return; // nada por cobrar
            c = CuentaPorCobrar.builder()
                .organizacionId(orden.getOrganizacionId()).orden(orden).cliente(orden.getCliente())
                .monto(totalEf).saldo(saldo)
                .fechaVencimiento(java.time.LocalDate.now().plusDays(parametros.entero("cuentas.plazo_cobrar_dias", orden.getOrganizacionId(), plazoCobrarDias)))
                .estado(saldo.compareTo(totalEf) >= 0 ? "pendiente" : "parcial").build();
        } else {
            c.setMonto(totalEf);
            c.setSaldo(saldo);
            c.setEstado(saldo.signum() <= 0 ? (totalEf.signum() <= 0 ? "anulada" : "pagada")
                                            : (saldo.compareTo(totalEf) >= 0 ? "pendiente" : "parcial"));
        }
        cxcRepo.save(c);
    }


    /** Filtros opcionales combinables (todos se aplican en la base de datos). */
    @lombok.Data @lombok.Builder
    public static class FiltroCuentas {
        private String estado;            // pendiente | parcial | pagada
        private Long proveedorId;         // solo cuentas por pagar
        private Long clienteId;           // solo cuentas por cobrar
        private java.time.LocalDate desde;
        private java.time.LocalDate hasta;
        private String campoFecha;        // "creacion" (por defecto) | "vencimiento"
        private Boolean soloVencidas;
    }

    private <T> org.springframework.data.jpa.domain.Specification<T> filtro(Long orgId, FiltroCuentas f, String tercero, Long terceroId) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("organizacionId"), orgId));
            if (f.getEstado() != null && !f.getEstado().isBlank()) ps.add(cb.equal(root.get("estado"), f.getEstado()));
            if (terceroId != null) ps.add(cb.equal(root.get(tercero).get("id"), terceroId));
            if ("vencimiento".equals(f.getCampoFecha())) {
                if (f.getDesde() != null) ps.add(cb.greaterThanOrEqualTo(root.<java.time.LocalDate>get("fechaVencimiento"), f.getDesde()));
                if (f.getHasta() != null) ps.add(cb.lessThanOrEqualTo(root.<java.time.LocalDate>get("fechaVencimiento"), f.getHasta()));
            } else {
                if (f.getDesde() != null) ps.add(cb.greaterThanOrEqualTo(root.<java.time.LocalDateTime>get("createdAt"), f.getDesde().atStartOfDay()));
                if (f.getHasta() != null) ps.add(cb.lessThan(root.<java.time.LocalDateTime>get("createdAt"), f.getHasta().plusDays(1).atStartOfDay()));
            }
            if (Boolean.TRUE.equals(f.getSoloVencidas())) {
                ps.add(cb.lessThan(root.<java.time.LocalDate>get("fechaVencimiento"), java.time.LocalDate.now()));
                ps.add(cb.greaterThan(root.<BigDecimal>get("saldo"), BigDecimal.ZERO));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ── helpers ──────────────────────────────────────────────
    /** Días transcurridos desde el vencimiento (0 si está al día o ya pagada). */
    public static int diasVencida(java.time.LocalDate vence, BigDecimal saldo) {
        if (vence == null || saldo == null || saldo.signum() <= 0) return 0;
        long d = java.time.temporal.ChronoUnit.DAYS.between(vence, java.time.LocalDate.now());
        return (int) Math.max(d, 0);
    }
    private CuentaPorPagar resolverCxp(Long id, Long orgId) {
        CuentaPorPagar c = cxpRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
        if (!c.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Cuenta no encontrada");
        return c;
    }
    private CuentaPorPagarDTO toCxpDTO(CuentaPorPagar c) {
        Unidad u = c.getUnidad();
        return CuentaPorPagarDTO.builder()
            .id(c.getId())
            .unidadId(u != null ? u.getId() : null)
            .unidadCodigo(u != null ? u.getCodigoUnico() : null)
            .productoNombre(u != null && u.getProducto() != null ? u.getProducto().getNombre() : null)
            .proveedorId(c.getProveedor() != null ? c.getProveedor().getId() : null)
            .proveedorNombre(c.getProveedor() != null ? c.getProveedor().getNombre() : null)
            .monto(c.getMonto()).saldo(c.getSaldo()).estado(c.getEstado())
            .descripcion(c.getDescripcion()).createdAt(c.getCreatedAt())
            .fechaVencimiento(c.getFechaVencimiento()).diasVencida(diasVencida(c.getFechaVencimiento(), c.getSaldo())).build();
    }
    private CuentaPorCobrarDTO toCxcDTO(CuentaPorCobrar c) {
        return CuentaPorCobrarDTO.builder()
            .id(c.getId())
            .ordenId(c.getOrden() != null ? c.getOrden().getId() : null)
            .ordenNumero(c.getOrden() != null ? c.getOrden().getNumeroOrden() : null)
            .clienteId(c.getCliente() != null ? c.getCliente().getId() : null)
            .clienteNombre(c.getCliente() != null ? c.getCliente().getNombre() : null)
            .monto(c.getMonto()).saldo(c.getSaldo()).estado(c.getEstado()).createdAt(c.getCreatedAt())
            .fechaVencimiento(c.getFechaVencimiento()).diasVencida(diasVencida(c.getFechaVencimiento(), c.getSaldo())).build();
    }
    private PagoProveedorDTO toPagoProvDTO(PagoProveedor p) {
        return PagoProveedorDTO.builder()
            .id(p.getId()).monto(p.getMonto()).metodo(p.getMetodo()).banco(p.getBanco())
            .comprobanteUrl(p.getComprobanteUrl()).ultimosDigitos(p.getUltimosDigitos())
            .fechaTransaccion(p.getFechaTransaccion()).referencia(p.getReferencia())
            .observaciones(p.getObservaciones()).createdAt(p.getCreatedAt()).build();
    }
}
