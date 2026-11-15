package com.saas.platform.user.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Registro de AUDITORÍA de operaciones críticas (tabla auditoria, solo inserción).
 * Se escribe en la MISMA transacción de la operación: si la operación falla, no queda registro,
 * y si el registro falla, la operación no se confirma.
 */
@Service
public class AuditoriaService {

    private static final String SERVICIO = "user";
    @PersistenceContext private EntityManager em;

    /** Registra la acción a nombre de quien hace la petición (organización tomada del usuario). */
    public void registrar(String accion, String entidad, Object entidadId, Map<String, ?> detalle) {
        registrar(accion, entidad, entidadId, detalle, null);
    }

    /** organizacionId explícito para procesos sin usuario (p. ej. tareas programadas). */
    public void registrar(String accion, String entidad, Object entidadId, Map<String, ?> detalle, Long organizacionId) {
        Object principal = principal();
        Long usuarioId = (Long) leer(principal, "getId");
        String email = (String) leer(principal, "getEmail");
        Long org = organizacionId != null ? organizacionId : (Long) leer(principal, "getOrganizacionId");
        if (org == null && usuarioId != null) {
            var r = em.createNativeQuery("SELECT organizacion_id FROM usuarios WHERE id = :u").setParameter("u", usuarioId).getResultList();
            if (!r.isEmpty() && r.get(0) != null) org = ((Number) r.get(0)).longValue();
        }
        HttpServletRequest req = request();
        em.createNativeQuery(
                "INSERT INTO auditoria (organizacion_id, usuario_id, usuario_email, servicio, accion, entidad, entidad_id, detalle, ip, user_agent) " +
                "VALUES (:org, :uid, :email, :srv, :acc, :ent, :eid, :det, :ip, :ua)")
            .setParameter("org", org).setParameter("uid", usuarioId).setParameter("email", email)
            .setParameter("srv", SERVICIO).setParameter("acc", accion).setParameter("ent", entidad)
            .setParameter("eid", entidadId != null ? String.valueOf(entidadId) : null)
            .setParameter("det", json(detalle))
            .setParameter("ip", req != null ? ip(req) : null)
            .setParameter("ua", req != null ? recortar(req.getHeader("User-Agent"), 300) : null)
            .executeUpdate();
    }

    // ── helpers ──────────────────────────────────────────────────────────
    private static Object principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getPrincipal() : null;
    }
    private static Object leer(Object o, String metodo) {
        if (o == null) return null;
        try { return o.getClass().getMethod(metodo).invoke(o); } catch (Exception e) { return null; }
    }
    private static HttpServletRequest request() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes s ? s.getRequest() : null;
    }
    private static String ip(HttpServletRequest r) {
        String f = r.getHeader("X-Forwarded-For");
        return recortar(f != null && !f.isBlank() ? f.split(",")[0].trim() : r.getRemoteAddr(), 64);
    }
    private static String recortar(String s, int max) { return s == null ? null : (s.length() > max ? s.substring(0, max) : s); }

    /** JSON simple y seguro (sin dependencias). Escapes con códigos de carácter para evitar ambigüedades. */
    static String json(Map<String, ?> m) {
        if (m == null || m.isEmpty()) return null;
        final char COMILLA = 34;
        StringBuilder sb = new StringBuilder().append('{');
        boolean primero = true;
        for (var e : m.entrySet()) {
            if (!primero) sb.append(',');
            primero = false;
            sb.append(COMILLA).append(esc(e.getKey())).append(COMILLA).append(':');
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append(COMILLA).append(esc(String.valueOf(v))).append(COMILLA);
        }
        return sb.append('}').toString();
    }

    private static String esc(String s) {
        final char BARRA = 92, COMILLA = 34;
        StringBuilder o = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == COMILLA || c == BARRA) o.append(BARRA).append(c);
            else if (c == 10) o.append(BARRA).append('n');
            else if (c == 13) o.append(BARRA).append('r');
            else if (c == 9)  o.append(BARRA).append('t');
            else if (c < 32)  o.append(BARRA).append('u').append(String.format("%04x", (int) c));
            else o.append(c);
        }
        return o.toString();
    }
}
