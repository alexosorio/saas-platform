package com.saas.platform.catalog.security;

import com.saas.platform.catalog.service.ParametrosLector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ¿La organización puede operar? Sí si es la súper organización (plataforma) o si su suscripción más
 * reciente está activa o en prueba Y su fecha de fin no ha pasado (más los días de gracia, 0 por defecto).
 * Se evalúa la FECHA, no solo el estado: el bloqueo es inmediato aunque el proceso programado aún no la
 * haya marcado como suspendida. Se cachea 60 s por organización para no consultar en cada petición.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SuscripcionVigente {

    private static final long TTL_MS = 60_000;
    private record Entrada(boolean vigente, long vence) {}

    private final Map<Long, Entrada> cache = new ConcurrentHashMap<>();
    private final ParametrosLector parametros;
    @PersistenceContext private EntityManager em;

    public boolean puedeOperar(Long orgId) {
        if (orgId == null) return true;
        long ahora = Instant.now().toEpochMilli();
        Entrada e = cache.get(orgId);
        if (e == null || e.vence() < ahora) {
            e = new Entrada(consultar(orgId), ahora + TTL_MS);
            cache.put(orgId, e);
        }
        return e.vigente();
    }

    /** Para que un cambio (p. ej. pago aprobado) se refleje sin esperar el minuto de caché. */
    public void olvidar(Long orgId) { if (orgId != null) cache.remove(orgId); }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    protected boolean consultar(Long orgId) {
        try {
            List<Object[]> r = em.createNativeQuery(
                    "SELECT COALESCE(o.es_plataforma, FALSE), CAST(s.estado AS text), s.fecha_fin FROM organizaciones o "
                  + "LEFT JOIN LATERAL (SELECT estado, fecha_fin FROM suscripciones WHERE organizacion_id = o.id "
                  + "                   ORDER BY created_at DESC, id DESC LIMIT 1) s ON TRUE WHERE o.id = :o")
                .setParameter("o", orgId).getResultList();
            if (r.isEmpty()) return false;
            Object[] f = r.get(0);
            if (Boolean.TRUE.equals(f[0])) return true;                       // súper organización
            String estado = f[1] == null ? null : String.valueOf(f[1]);
            if (!"activa".equals(estado) && !"periodo_prueba".equals(estado)) return false;
            if (f[2] == null) return true;                                     // sin fecha de fin
            LocalDate fin = f[2] instanceof java.sql.Date d ? d.toLocalDate() : (LocalDate) f[2];
            int gracia = Math.max(0, parametros.entero("suscripcion.dias_gracia", null, 0));
            return !fin.plusDays(gracia).isBefore(LocalDate.now());
        } catch (Exception ex) {
            // Si no se puede comprobar (BD caída, tabla ausente) no se bloquea a todos por un error técnico
            log.error("No se pudo comprobar la suscripción de la organización {}: {}", orgId, ex.getMessage());
            return true;
        }
    }
}
