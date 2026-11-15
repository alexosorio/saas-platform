package com.saas.platform.catalog.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.*;

/**
 * Resume por PRODUCTO la información que vive en sus UNIDADES disponibles:
 * colores, precios de compra/venta y stock. Cada método hace UNA consulta para
 * todos los productos (evita N+1). Si se pasa productoId, se limita a ese producto.
 */
@Service
public class ResumenUnidadesService {

    @PersistenceContext
    private EntityManager em;

    /** Precios de una unidad disponible. */
    public record PrecioUnidad(BigDecimal venta, BigDecimal compra) {}

    private static final String DISPONIBLE = "CAST(u.estado AS varchar) = 'disponible'";

    /** Colores (característica 'color') de las unidades disponibles, normalizados y sin duplicados. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<Long, List<String>> coloresDisponibles(Long orgId, Long productoId) {
        var q = em.createNativeQuery(
            "SELECT u.producto_id, vc.valor FROM valores_caracteristicas vc " +
            "JOIN caracteristicas_productos cp ON cp.id = vc.caracteristica_id " +
            "JOIN unidades u ON u.id = vc.unidad_id " +
            "JOIN productos p ON p.id = u.producto_id " +
            "WHERE p.organizacion_id = :org AND LOWER(cp.codigo) = 'color' AND " + DISPONIBLE +
            " AND TRIM(COALESCE(vc.valor, '')) <> ''" +
            (productoId != null ? " AND p.id = :prod" : ""))
            .setParameter("org", orgId);
        if (productoId != null) q.setParameter("prod", productoId);

        Map<Long, Map<String, String>> porProducto = new HashMap<>();   // clave normalizada -> texto a mostrar
        for (Object[] f : (List<Object[]>) q.getResultList()) {
            String mostrado = normalizarColor((String) f[1]);
            porProducto.computeIfAbsent(((Number) f[0]).longValue(), k -> new LinkedHashMap<>())
                       .putIfAbsent(mostrado.toLowerCase(Locale.ROOT), mostrado);
        }
        Collator es = Collator.getInstance(new Locale("es"));
        Map<Long, List<String>> out = new HashMap<>();
        porProducto.forEach((id, m) -> { List<String> l = new ArrayList<>(m.values()); l.sort(es); out.put(id, l); });
        return out;
    }

    /** Precios de venta y compra de cada unidad disponible, agrupados por producto. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<Long, List<PrecioUnidad>> preciosDisponibles(Long orgId, Long productoId) {
        var q = em.createNativeQuery(
            "SELECT u.producto_id, u.precio_venta, u.precio_compra FROM unidades u " +
            "JOIN productos p ON p.id = u.producto_id " +
            "WHERE p.organizacion_id = :org AND " + DISPONIBLE +
            (productoId != null ? " AND p.id = :prod" : ""))
            .setParameter("org", orgId);
        if (productoId != null) q.setParameter("prod", productoId);
        Map<Long, List<PrecioUnidad>> out = new HashMap<>();
        for (Object[] f : (List<Object[]>) q.getResultList())
            out.computeIfAbsent(((Number) f[0]).longValue(), k -> new ArrayList<>())
               .add(new PrecioUnidad(dec(f[1]), dec(f[2])));
        return out;
    }

    /** Stock disponible (existencias - reservado) por producto. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<Long, Integer> stockPorProducto(Long orgId, Long productoId) {
        var q = em.createNativeQuery(
            "SELECT u.producto_id, COALESCE(SUM(i.cantidad - COALESCE(i.cantidad_reservada, 0)), 0) " +
            "FROM inventario i JOIN unidades u ON u.id = i.unidad_id JOIN productos p ON p.id = u.producto_id " +
            "WHERE p.organizacion_id = :org" + (productoId != null ? " AND p.id = :prod" : "") +
            " GROUP BY u.producto_id")
            .setParameter("org", orgId);
        if (productoId != null) q.setParameter("prod", productoId);
        Map<Long, Integer> out = new HashMap<>();
        for (Object[] f : (List<Object[]>) q.getResultList())
            out.put(((Number) f[0]).longValue(), ((Number) f[1]).intValue());
        return out;
    }

    /** "  negro   MATE " -> "Negro mate" */
    public static String normalizarColor(String v) {
        String t = v.trim().replaceAll("\\s+", " ").toLowerCase(new Locale("es"));
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static BigDecimal dec(Object o) {
        if (o == null) return null;
        return o instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(o));
    }
}
