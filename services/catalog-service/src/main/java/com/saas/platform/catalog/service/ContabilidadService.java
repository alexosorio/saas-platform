package com.saas.platform.catalog.service;

import com.saas.platform.catalog.entity.AsientoContable;
import com.saas.platform.catalog.entity.MovimientoContable;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.repository.AsientoContableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Asientos contables por partida doble. Valida que cuadren antes de guardar
 * (además, la base de datos lo exige con una restricción CHECK).
 */
@Service
@RequiredArgsConstructor
public class ContabilidadService {

    /** Cuenta por defecto (PUC) de cada parámetro, por si la organización no la configuró. */
    private static final Map<String, String> POR_DEFECTO = Map.of(
        "contab.devoluciones_ventas", "4175 Devoluciones en ventas",
        "contab.iva_por_pagar", "2408 Impuesto sobre las ventas por pagar",
        "contab.clientes", "1305 Clientes",
        "contab.caja", "1105 Caja",
        "contab.bancos", "1110 Bancos",
        "contab.anticipos_clientes", "2805 Anticipos y avances recibidos",
        "contab.inventario", "1435 Mercancías no fabricadas por la empresa",
        "contab.costo_ventas", "6135 Comercio al por mayor y al por menor",
        "contab.perdida_inventario", "5315 Gastos extraordinarios");

    private final AsientoContableRepository asientoRepo;
    private final ParametrosLector parametros;
    private final AuditoriaService auditoria;

    /** Línea de un asiento (usar debe() o haber()). */
    public record Linea(String clave, BigDecimal debe, BigDecimal haber, String tercero, String detalle) {
        public static Linea debe(String clave, BigDecimal v, String tercero, String detalle) { return new Linea(clave, v, BigDecimal.ZERO, tercero, detalle); }
        public static Linea haber(String clave, BigDecimal v, String tercero, String detalle) { return new Linea(clave, BigDecimal.ZERO, v, tercero, detalle); }
    }

    @Transactional
    public AsientoContable registrar(Long orgId, String tipo, String referencia, Long documentoId,
                                     String descripcion, List<Linea> lineas, Long usuarioId) {
        List<MovimientoContable> movs = new ArrayList<>();
        BigDecimal debe = BigDecimal.ZERO, haber = BigDecimal.ZERO;
        for (Linea l : lineas) {
            BigDecimal d = esc(l.debe()), h = esc(l.haber());
            if (d.signum() == 0 && h.signum() == 0) continue;          // líneas en cero no se registran
            String[] cuenta = cuenta(l.clave(), orgId);
            movs.add(MovimientoContable.builder().cuentaCodigo(cuenta[0]).cuentaNombre(cuenta[1])
                .debe(d).haber(h).tercero(l.tercero()).detalle(l.detalle()).build());
            debe = debe.add(d); haber = haber.add(h);
        }
        if (movs.isEmpty()) return null;
        if (debe.compareTo(haber) != 0)
            throw new InvalidOperationException("El asiento contable no cuadra (débitos " + debe + " ≠ créditos " + haber + ")");

        AsientoContable a = AsientoContable.builder().organizacionId(orgId).tipo(tipo).referencia(referencia)
            .documentoId(documentoId).descripcion(descripcion).totalDebe(debe).totalHaber(haber).usuarioId(usuarioId).build();
        movs.forEach(m -> m.setAsiento(a));
        a.getMovimientos().addAll(movs);
        AsientoContable guardado = asientoRepo.save(a);
        auditoria.registrar("ASIENTO_CONTABLE", "asiento", guardado.getId(), Map.of(
            "tipo", tipo, "referencia", referencia, "total", debe, "movimientos", movs.size()));
        return guardado;
    }

    /** {código, nombre} de la cuenta configurada: valor "1305 Clientes" -> ["1305", "Clientes"]. */
    private String[] cuenta(String clave, Long orgId) {
        String v = parametros.texto(clave, orgId, POR_DEFECTO.getOrDefault(clave, clave));
        String[] p = v.trim().split("\\s+", 2);
        return new String[]{ p[0], p.length > 1 ? p[1] : "" };
    }

    private static BigDecimal esc(BigDecimal v) { return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP); }
}
