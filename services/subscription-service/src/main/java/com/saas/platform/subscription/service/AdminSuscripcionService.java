package com.saas.platform.subscription.service;

import com.saas.platform.subscription.dto.AdminDTOs.*;
import com.saas.platform.subscription.entity.HistorialSuscripcion;
import com.saas.platform.subscription.entity.Pago;
import com.saas.platform.subscription.entity.Suscripcion;
import com.saas.platform.subscription.exception.ResourceNotFoundException;
import com.saas.platform.subscription.repository.HistorialSuscripcionRepository;
import com.saas.platform.subscription.repository.PagoRepository;
import com.saas.platform.subscription.repository.SuscripcionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Administración de suscripciones a nivel de PLATAFORMA (todas las organizaciones):
 * validación de pagos, activación/suspensión manual, facturación y suspensión automática por no pago.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSuscripcionService {

    private final SuscripcionRepository suscripcionRepo;
    private final PagoRepository pagoRepo;
    private final HistorialSuscripcionRepository historialRepo;
    private final ParametrosLector parametros;
    private final AuditoriaService auditoria;
    private final VencimientoService vencimientos;
    @PersistenceContext private EntityManager em;

    /** Días de gracia después del vencimiento antes de suspender automáticamente. */
    @Value("${app.suscripciones.dias-gracia:3}")
    private int diasGracia;

    // ── Consultas ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SuscripcionAdmin> listarSuscripciones(String estado, String q) {
        StringBuilder sql = new StringBuilder(
            "SELECT s.id, s.organizacion_id, o.nombre, o.email, pl.nombre, CAST(s.periodo AS varchar), " +
            "CAST(s.estado AS varchar), s.fecha_inicio, s.fecha_fin, s.created_at, " +
            "(SELECT COUNT(*) FROM pagos pg WHERE pg.suscripcion_id = s.id AND CAST(pg.estado AS varchar) = 'pendiente') " +
            "FROM suscripciones s JOIN organizaciones o ON o.id = s.organizacion_id " +
            "LEFT JOIN planes pl ON pl.id = s.plan_id WHERE 1 = 1");
        if (estado != null && !estado.isBlank()) sql.append(" AND CAST(s.estado AS varchar) = :estado");
        if (q != null && !q.isBlank()) sql.append(" AND (LOWER(o.nombre) LIKE :q OR LOWER(COALESCE(o.email, '')) LIKE :q)");
        sql.append(" ORDER BY s.created_at DESC");
        var query = em.createNativeQuery(sql.toString());
        if (estado != null && !estado.isBlank()) query.setParameter("estado", estado);
        if (q != null && !q.isBlank()) query.setParameter("q", "%" + q.trim().toLowerCase() + "%");

        List<SuscripcionAdmin> out = new ArrayList<>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            LocalDate fin = aFecha(f[8]);
            out.add(SuscripcionAdmin.builder()
                .id(num(f[0])).organizacionId(num(f[1])).organizacionNombre((String) f[2]).organizacionEmail((String) f[3])
                .planNombre((String) f[4]).periodo((String) f[5]).estado((String) f[6])
                .fechaInicio(aFecha(f[7])).fechaFin(fin)
                .diasParaVencer(fin != null ? (int) ChronoUnit.DAYS.between(LocalDate.now(), fin) : null)
                .createdAt(aFechaHora(f[9])).pagosPendientes(num(f[10])).build());
        }
        return out;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<PagoAdmin> listarPagos(String estado) {
        String sql = "SELECT pg.id, pg.suscripcion_id, o.nombre, pl.nombre, pg.monto, pg.moneda, CAST(pg.estado AS varchar), " +
            "pg.metodo, pg.banco, pg.referencia_externa, pg.ultimos_digitos, pg.comprobante_url, pg.fecha_transaccion, " +
            "pg.motivo_rechazo, pg.fecha_validacion, pg.created_at " +
            "FROM pagos pg JOIN suscripciones s ON s.id = pg.suscripcion_id " +
            "JOIN organizaciones o ON o.id = s.organizacion_id LEFT JOIN planes pl ON pl.id = s.plan_id " +
            ((estado != null && !estado.isBlank()) ? "WHERE CAST(pg.estado AS varchar) = :estado " : "") +
            "ORDER BY pg.created_at DESC LIMIT 300";
        var query = em.createNativeQuery(sql);
        if (estado != null && !estado.isBlank()) query.setParameter("estado", estado);
        List<PagoAdmin> out = new ArrayList<>();
        for (Object[] f : (List<Object[]>) query.getResultList()) {
            out.add(PagoAdmin.builder()
                .id(num(f[0])).suscripcionId(num(f[1])).organizacionNombre((String) f[2]).planNombre((String) f[3])
                .monto(dec(f[4])).moneda((String) f[5]).estado((String) f[6]).metodo((String) f[7]).banco((String) f[8])
                .referencia((String) f[9]).ultimosDigitos((String) f[10]).comprobanteUrl((String) f[11])
                .fechaTransaccion(aFecha(f[12])).motivoRechazo((String) f[13])
                .fechaValidacion(aFechaHora(f[14])).createdAt(aFechaHora(f[15])).build());
        }
        return out;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Facturacion facturacion(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfYear(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        String base = " FROM pagos pg JOIN suscripciones s ON s.id = pg.suscripcion_id LEFT JOIN planes pl ON pl.id = s.plan_id " +
                      "WHERE CAST(pg.estado AS varchar) = 'completado' AND COALESCE(pg.fecha_pago, CAST(pg.created_at AS date)) BETWEEN :d AND :h";

        List<Fila> porMes = filas(em.createNativeQuery(
            "SELECT to_char(COALESCE(pg.fecha_pago, CAST(pg.created_at AS date)), 'YYYY-MM') AS mes, SUM(pg.monto), COUNT(*)" + base +
            " GROUP BY mes ORDER BY mes").setParameter("d", d).setParameter("h", h).getResultList());
        List<Fila> porPlan = filas(em.createNativeQuery(
            "SELECT COALESCE(pl.nombre, 'Sin plan'), SUM(pg.monto), COUNT(*)" + base +
            " GROUP BY pl.nombre ORDER BY SUM(pg.monto) DESC").setParameter("d", d).setParameter("h", h).getResultList());

        BigDecimal total = porMes.stream().map(Fila::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long cantidad = porMes.stream().mapToLong(Fila::getCantidad).sum();
        long activas = num(em.createNativeQuery(
            "SELECT COUNT(*) FROM suscripciones WHERE CAST(estado AS varchar) = 'activa'").getSingleResult());
        long pendientes = num(em.createNativeQuery(
            "SELECT COUNT(*) FROM pagos WHERE CAST(estado AS varchar) = 'pendiente'").getSingleResult());

        return Facturacion.builder()
            .totalFacturado(total).pagosAprobados(cantidad)
            .ticketPromedio(cantidad > 0 ? total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
            .suscripcionesActivas(activas).pendientesValidacion(pendientes)
            .porMes(porMes).porPlan(porPlan).build();
    }

    // ── Validación de pagos ──────────────────────────────────────────────

    /** Aprueba el pago: activa la suscripción y extiende su vigencia un período. */
    @Transactional
    public PagoAdmin aprobarPago(Long pagoId, Long adminId) {
        Pago p = pagoPendiente(pagoId);
        Suscripcion s = suscripcionRepo.findById(p.getSuscripcionId())
            .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));

        p.setEstado(Pago.Estado.completado);
        p.setValidadoPor(adminId);
        p.setFechaValidacion(LocalDateTime.now());
        p.setFechaPago(p.getFechaTransaccion() != null ? p.getFechaTransaccion() : LocalDate.now());
        pagoRepo.save(p);

        // La nueva vigencia parte del vencimiento actual si aún no ha pasado; si no, de hoy
        LocalDate base = (s.getFechaFin() != null && s.getFechaFin().isAfter(LocalDate.now())) ? s.getFechaFin() : LocalDate.now();
        s.setFechaFin(extender(base, s.getPeriodo()));
        if (s.getFechaInicio() == null) s.setFechaInicio(LocalDate.now());
        s.setEstado(Suscripcion.Estado.activa);
        suscripcionRepo.save(s);
        historial(s, HistorialSuscripcion.Accion.renovacion,
            "Pago " + p.getReferenciaInterna() + " validado. Vigente hasta " + s.getFechaFin(), adminId, p.getMonto());
        log.info("Pago {} aprobado por {}; suscripción {} activa hasta {}", pagoId, adminId, s.getId(), s.getFechaFin());
        auditoria.registrar("PAGO_SUSCRIPCION_APROBADO", "pago", pagoId, java.util.Map.of("monto", p.getMonto(), "suscripcionId", s.getId(), "vigenteHasta", String.valueOf(s.getFechaFin())), s.getOrganizacionId());
        return listarPagos(null).stream().filter(x -> x.getId().equals(pagoId)).findFirst().orElse(null);
    }

    @Transactional
    public PagoAdmin rechazarPago(Long pagoId, String motivo, Long adminId) {
        if (motivo == null || motivo.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el motivo del rechazo");
        Pago p = pagoPendiente(pagoId);
        p.setEstado(Pago.Estado.fallido);
        p.setMotivoRechazo(motivo.trim());
        p.setValidadoPor(adminId);
        p.setFechaValidacion(LocalDateTime.now());
        pagoRepo.save(p);
        log.info("Pago {} rechazado por {}: {}", pagoId, adminId, motivo);
        auditoria.registrar("PAGO_SUSCRIPCION_RECHAZADO", "pago", pagoId, java.util.Map.of("monto", p.getMonto(), "motivo", motivo.trim()));
        return listarPagos(null).stream().filter(x -> x.getId().equals(pagoId)).findFirst().orElse(null);
    }

    // ── Activación / suspensión manual ───────────────────────────────────

    @Transactional
    public void activar(Long suscripcionId, String motivo, Long adminId) {
        Suscripcion s = suscripcionRepo.findById(suscripcionId)
            .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
        if (s.getEstado() == Suscripcion.Estado.activa)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suscripción ya está activa");
        if (s.getFechaFin() == null || s.getFechaFin().isBefore(LocalDate.now()))
            s.setFechaFin(extender(LocalDate.now(), s.getPeriodo()));
        if (s.getFechaInicio() == null) s.setFechaInicio(LocalDate.now());
        s.setEstado(Suscripcion.Estado.activa);
        suscripcionRepo.save(s);
        historial(s, HistorialSuscripcion.Accion.reactivacion,
            "Activación manual" + (motivo != null && !motivo.isBlank() ? ": " + motivo.trim() : ""), adminId, null);
            auditoria.registrar("SUSCRIPCION_ACTIVADA", "suscripcion", s.getId(), java.util.Map.of("motivo", String.valueOf(motivo)), s.getOrganizacionId());
    }

    @Transactional
    public void suspender(Long suscripcionId, String motivo, Long adminId) {
        if (motivo == null || motivo.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el motivo de la suspensión");
        Suscripcion s = suscripcionRepo.findById(suscripcionId)
            .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
        if (s.getEstado() == Suscripcion.Estado.suspendida)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suscripción ya está suspendida");
        s.setEstado(Suscripcion.Estado.suspendida);
        suscripcionRepo.save(s);
        historial(s, HistorialSuscripcion.Accion.suspension, "Suspensión manual: " + motivo.trim(), adminId, null);
            auditoria.registrar("SUSCRIPCION_SUSPENDIDA", "suscripcion", s.getId(), java.util.Map.of("motivo", motivo.trim()), s.getOrganizacionId());
    }

    // ── Suspensión automática por no pago ────────────────────────────────

    /**
     * Suspende las suscripciones activas o en prueba cuyo vencimiento pasó hace más de
     * 'dias-gracia' días. Lo ejecuta el programador diario y también puede lanzarse a mano.
     */
    @Transactional
    public ResultadoVencimientos procesarVencimientos() {
        // Misma regla que al consultar la suscripción: vencida = suspendida de inmediato (con aviso por correo)
        List<String> detalle = new ArrayList<>();
        for (Suscripcion s : suscripcionRepo.findAll()) {
            if (vencimientos.suspenderSiVencida(s))
                detalle.add("Suscripción " + s.getId() + " (organización " + s.getOrganizacionId() + "): venció el " + s.getFechaFin());
        }
        if (!detalle.isEmpty()) log.warn("Vencimientos: {} suscripción(es) suspendida(s)", detalle.size());
        return ResultadoVencimientos.builder().suspendidas(detalle.size()).detalle(detalle).build();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Pago pagoPendiente(Long id) {
        Pago p = pagoRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        if (p.getEstado() != Pago.Estado.pendiente)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago ya fue validado (estado: " + p.getEstado() + ")");
        return p;
    }

    private static LocalDate extender(LocalDate base, Suscripcion.Periodo periodo) {
        return periodo == Suscripcion.Periodo.anual ? base.plusYears(1) : base.plusMonths(1);
    }

    private void historial(Suscripcion s, HistorialSuscripcion.Accion accion, String motivo, Long usuarioId, BigDecimal monto) {
        historialRepo.save(HistorialSuscripcion.builder()
            .suscripcionId(s.getId())
            .planAnteriorId(s.getPlan() != null ? s.getPlan().getId() : null)
            .planNuevoId(s.getPlan() != null ? s.getPlan().getId() : null)
            .accion(accion).motivo(motivo).usuarioId(usuarioId).montoCambio(monto).build());
    }

    private static List<Fila> filas(List<?> rows) {
        List<Fila> out = new ArrayList<>();
        for (Object o : rows) {
            Object[] f = (Object[]) o;
            out.add(Fila.builder().etiqueta(String.valueOf(f[0])).total(dec(f[1])).cantidad(num(f[2])).build());
        }
        return out;
    }
    private static long num(Object o) { return o == null ? 0 : ((Number) o).longValue(); }
    private static BigDecimal dec(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return o instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(o));
    }
    private static LocalDate aFecha(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDateTime t) return t.toLocalDate();
        return LocalDate.parse(String.valueOf(o).substring(0, 10));
    }
    private static LocalDateTime aFechaHora(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime t) return t;
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime();
        if (o instanceof OffsetDateTime t) return t.toLocalDateTime();
        if (o instanceof Instant i) return LocalDateTime.ofInstant(i, ZoneId.systemDefault());
        return LocalDateTime.parse(String.valueOf(o).replace(' ', 'T'));
    }
}
