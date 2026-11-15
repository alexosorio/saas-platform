package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.DashboardDTO;
import com.saas.platform.catalog.dto.DashboardDTO.*;
import com.saas.platform.catalog.repository.CuentaPorCobrarRepository;
import com.saas.platform.catalog.repository.CuentaPorPagarRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String[] RANGOS = {"Al día", "1-30 días", "31-60 días", "61-90 días", "+90 días"};

    private final CuentaPorPagarRepository cxpRepo;
    private final CuentaPorCobrarRepository cxcRepo;
    @PersistenceContext private EntityManager em;

    @Transactional(readOnly = true)
    public DashboardDTO resumen(Long orgId, int dias, int umbral) {
        dias = Math.max(1, Math.min(dias, 365));
        umbral = Math.max(0, umbral);

        var cxp = cxpRepo.findByOrganizacionIdAndSaldoGreaterThan(orgId, BigDecimal.ZERO);
        var cxc = cxcRepo.findByOrganizacionIdAndSaldoGreaterThan(orgId, BigDecimal.ZERO);
        List<RangoCartera> carteraPagar  = cartera(cxp, c -> c.getFechaVencimiento(), c -> c.getSaldo());
        List<RangoCartera> carteraCobrar = cartera(cxc, c -> c.getFechaVencimiento(), c -> c.getSaldo());
        List<IngresoDia> ingresos = ingresosPorDia(orgId, dias);

        Totales tot = Totales.builder()
            .porPagar(suma(carteraPagar, false)).vencidoPorPagar(suma(carteraPagar, true))
            .porCobrar(suma(carteraCobrar, false)).vencidoPorCobrar(suma(carteraCobrar, true))
            .ingresosPeriodo(ingresos.stream().map(IngresoDia::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add))
            .diasPeriodo(dias).umbralStock(umbral).build();

        return DashboardDTO.builder()
            .totales(tot)
            .porPagarPorProveedor(porProveedor(orgId))
            .carteraPorPagar(carteraPagar).carteraPorCobrar(carteraCobrar)
            .ingresosPorDia(ingresos)
            .recienLlegados(recienLlegados(orgId))
            .stockBajo(stockBajo(orgId, umbral))
            .build();
    }

    // ── Cartera por edades según días de vencimiento ─────────────────────
    private <T> List<RangoCartera> cartera(List<T> cuentas, Function<T, LocalDate> vence, Function<T, BigDecimal> saldo) {
        BigDecimal[] sumas = new BigDecimal[RANGOS.length];
        long[] cant = new long[RANGOS.length];
        Arrays.fill(sumas, BigDecimal.ZERO);
        for (T c : cuentas) {
            int d = CuentaService.diasVencida(vence.apply(c), saldo.apply(c));
            int i = d <= 0 ? 0 : d <= 30 ? 1 : d <= 60 ? 2 : d <= 90 ? 3 : 4;
            sumas[i] = sumas[i].add(saldo.apply(c)); cant[i]++;
        }
        List<RangoCartera> out = new ArrayList<>();
        for (int i = 0; i < RANGOS.length; i++)
            out.add(RangoCartera.builder().rango(RANGOS[i]).saldo(sumas[i]).cuentas(cant[i]).build());
        return out;
    }
    private BigDecimal suma(List<RangoCartera> r, boolean soloVencido) {
        return r.stream().skip(soloVencido ? 1 : 0).map(RangoCartera::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ProveedorSaldo> porProveedor(Long orgId) {
        List<ProveedorSaldo> out = new ArrayList<>();
        for (Object[] r : cxpRepo.saldoPorProveedor(orgId)) {
            out.add(ProveedorSaldo.builder()
                .proveedor(r[0] != null ? (String) r[0] : "Sin proveedor")
                .saldo((BigDecimal) r[1]).cuentas(((Number) r[2]).longValue()).build());
        }
        return out;
    }

    // ── Ingresos: pagos recibidos de órdenes, por día (días sin pagos = 0) ─
    @SuppressWarnings("unchecked")
    private List<IngresoDia> ingresosPorDia(Long orgId, int dias) {
        LocalDate desde = LocalDate.now().minusDays(dias - 1L);
        List<Object[]> filas = em.createNativeQuery(
            "SELECT CAST(po.fecha_pago AS date) AS dia, SUM(po.monto) FROM pagos_orden po " +
            "JOIN ordenes o ON o.id = po.orden_id " +
            "WHERE o.organizacion_id = :org AND po.fecha_pago >= :desde " +
            "GROUP BY CAST(po.fecha_pago AS date) ORDER BY dia")
            .setParameter("org", orgId).setParameter("desde", desde.atStartOfDay())
            .getResultList();
        Map<LocalDate, BigDecimal> porDia = new HashMap<>();
        for (Object[] f : filas) porDia.put(aFecha(f[0]), aDecimal(f[1]));
        List<IngresoDia> out = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(LocalDate.now()); d = d.plusDays(1))
            out.add(IngresoDia.builder().fecha(d).monto(porDia.getOrDefault(d, BigDecimal.ZERO)).build());
        return out;
    }

    // ── Últimas unidades que ingresaron y siguen disponibles ─────────────
    @SuppressWarnings("unchecked")
    private List<UnidadReciente> recienLlegados(Long orgId) {
        List<Object[]> filas = em.createNativeQuery(
            "SELECT u.id, u.codigo_unico, p.nombre, pr.nombre, " +
            "  (SELECT b.nombre FROM inventario i JOIN bodegas b ON b.id = i.bodega_id " +
            "    WHERE i.unidad_id = u.id ORDER BY i.cantidad DESC LIMIT 1), u.created_at " +
            "FROM unidades u JOIN productos p ON p.id = u.producto_id " +
            "LEFT JOIN proveedores pr ON pr.id = u.proveedor_id " +
            "WHERE p.organizacion_id = :org AND CAST(u.estado AS varchar) = 'disponible' " +
            "ORDER BY u.created_at DESC LIMIT 10")
            .setParameter("org", orgId).getResultList();
        List<UnidadReciente> out = new ArrayList<>();
        for (Object[] f : filas) out.add(UnidadReciente.builder()
            .unidadId(((Number) f[0]).longValue()).codigo((String) f[1]).producto((String) f[2])
            .proveedor((String) f[3]).bodega((String) f[4]).fecha(aFechaHora(f[5])).build());
        return out;
    }

    // ── Productos activos con disponible (existencias - reservado) <= umbral ─
    @SuppressWarnings("unchecked")
    private List<StockBajo> stockBajo(Long orgId, int umbral) {
        List<Object[]> filas = em.createNativeQuery(
            "SELECT p.id, p.nombre, COALESCE(SUM(i.cantidad - COALESCE(i.cantidad_reservada, 0)), 0) AS disp " +
            "FROM productos p LEFT JOIN unidades u ON u.producto_id = p.id " +
            "LEFT JOIN inventario i ON i.unidad_id = u.id " +
            "WHERE p.organizacion_id = :org AND p.activo = true " +
            "GROUP BY p.id, p.nombre " +
            "HAVING COALESCE(SUM(i.cantidad - COALESCE(i.cantidad_reservada, 0)), 0) <= :umbral " +
            "ORDER BY disp ASC, p.nombre LIMIT 15")
            .setParameter("org", orgId).setParameter("umbral", umbral).getResultList();
        List<StockBajo> out = new ArrayList<>();
        for (Object[] f : filas) out.add(StockBajo.builder()
            .productoId(((Number) f[0]).longValue()).producto((String) f[1])
            .disponible(((Number) f[2]).longValue()).build());
        return out;
    }

    // ── Conversión robusta de tipos devueltos por consultas nativas ─────
    private static LocalDate aFecha(Object o) {
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
    private static BigDecimal aDecimal(Object o) {
        if (o instanceof BigDecimal b) return b;
        return o == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(o));
    }
}
