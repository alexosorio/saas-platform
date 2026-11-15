package com.saas.platform.user.controller;

import com.saas.platform.user.security.ContextoActor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/** Consulta de auditoría: cada organización ve la suya; la súper organización ve todas. */
@RestController
@RequiredArgsConstructor
public class AuditoriaController {
    private final ContextoActor actor;
    @PersistenceContext private EntityManager em;

    private static final String[] COLS = {"id", "fecha", "organizacionId", "organizacion", "usuario", "servicio", "accion", "entidad", "entidadId", "detalle", "ip"};

    /** Compatibilidad: últimos 500 registros sin paginar. */
    @GetMapping("/api/auditoria")
    @PreAuthorize("@authz.can('auditoria','leer')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String usuario) {
        Map<String, Object> p = new HashMap<>();
        String where = filtros(desde, hasta, accion, entidad, usuario, p);
        return filas(where + " ORDER BY a.fecha DESC LIMIT 500", p);
    }

    /** Auditoría paginada. ?page=0&size=20 + los mismos filtros. */
    @GetMapping("/api/auditoria/paginado")
    @PreAuthorize("@authz.can('auditoria','leer')")
    @Transactional(readOnly = true)
    public Map<String, Object> paginado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String usuario,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        int pagina = page == null || page < 0 ? 0 : page;
        int tamano = size == null || size < 1 ? 20 : Math.min(size, 100);
        Map<String, Object> p = new HashMap<>();
        String where = filtros(desde, hasta, accion, entidad, usuario, p);

        var cq = em.createNativeQuery("SELECT COUNT(*) FROM auditoria a WHERE 1 = 1" + where);
        p.forEach(cq::setParameter);
        long total = ((Number) cq.getSingleResult()).longValue();

        Map<String, Object> pp = new HashMap<>(p);
        pp.put("lim", tamano); pp.put("off", (long) pagina * tamano);
        List<Map<String, Object>> contenido = total == 0 ? List.of()
            : filas(where + " ORDER BY a.fecha DESC, a.id DESC LIMIT :lim OFFSET :off", pp);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("contenido", contenido);
        r.put("pagina", pagina);
        r.put("tamano", tamano);
        r.put("totalElementos", total);
        r.put("totalPaginas", (int) ((total + tamano - 1) / tamano));
        return r;
    }

    /** Condiciones comunes (cada organización ve solo lo suyo; la plataforma, todo). */
    private String filtros(LocalDate desde, LocalDate hasta, String accion, String entidad, String usuario, Map<String, Object> p) {
        StringBuilder sql = new StringBuilder();
        if (!actor.esPlataforma()) { sql.append(" AND a.organizacion_id = :org"); p.put("org", actor.organizacionId()); }
        if (desde != null) { sql.append(" AND a.fecha >= :d"); p.put("d", desde.atStartOfDay()); }
        if (hasta != null) { sql.append(" AND a.fecha < :h"); p.put("h", hasta.plusDays(1).atStartOfDay()); }
        if (accion != null && !accion.isBlank()) { sql.append(" AND a.accion = :acc"); p.put("acc", accion); }
        if (entidad != null && !entidad.isBlank()) { sql.append(" AND a.entidad = :ent"); p.put("ent", entidad); }
        if (usuario != null && !usuario.isBlank()) { sql.append(" AND LOWER(a.usuario_email) LIKE :usr"); p.put("usr", "%" + usuario.trim().toLowerCase() + "%"); }
        return sql.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filas(String whereYOrden, Map<String, Object> p) {
        var q = em.createNativeQuery(
            "SELECT a.id, a.fecha, a.organizacion_id, o.nombre, a.usuario_email, a.servicio, a.accion, a.entidad, a.entidad_id, a.detalle, a.ip " +
            "FROM auditoria a LEFT JOIN organizaciones o ON o.id = a.organizacion_id WHERE 1 = 1" + whereYOrden);
        p.forEach(q::setParameter);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] f : (List<Object[]>) q.getResultList()) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < COLS.length; i++) m.put(COLS[i], f[i] != null ? String.valueOf(f[i]) : null);
            out.add(m);
        }
        return out;
    }
}
