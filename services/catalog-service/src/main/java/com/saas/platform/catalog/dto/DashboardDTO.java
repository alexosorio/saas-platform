package com.saas.platform.catalog.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardDTO {
    private Totales totales;
    private List<ProveedorSaldo> porPagarPorProveedor;
    private List<RangoCartera> carteraPorPagar;     // al día, 1-30, 31-60, 61-90, +90 días vencida
    private List<RangoCartera> carteraPorCobrar;
    private List<IngresoDia> ingresosPorDia;
    private List<UnidadReciente> recienLlegados;
    private List<StockBajo> stockBajo;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Totales {
        private BigDecimal porPagar, vencidoPorPagar, porCobrar, vencidoPorCobrar, ingresosPeriodo;
        private int diasPeriodo, umbralStock;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProveedorSaldo { private String proveedor; private BigDecimal saldo; private long cuentas; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RangoCartera { private String rango; private BigDecimal saldo; private long cuentas; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngresoDia { private LocalDate fecha; private BigDecimal monto; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UnidadReciente {
        private Long unidadId; private String codigo; private String producto;
        private String proveedor; private String bodega; private LocalDateTime fecha;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockBajo { private Long productoId; private String producto; private long disponible; }
}
