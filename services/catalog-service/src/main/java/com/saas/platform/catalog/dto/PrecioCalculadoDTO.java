package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrecioCalculadoDTO {
    private Long productoId;
    private String nombre;
    private String sku;
    private String marcaNombre;
    private String categoriaNombre;
    private String imagenUrl;
    private BigDecimal precioCalculado;   // precio "desde" (mínimo entre las unidades disponibles, o el override)
    private BigDecimal precioMinimo;      // rango de precio según las unidades disponibles
    private BigDecimal precioMaximo;
    private Boolean esOverride;           // true si vino de la tabla precios (excepción manual del producto)
    private Integer stockTotal;
    private Integer unidadesDisponibles;
    private List<String> colores;         // colores disponibles (característica 'color' de las unidades)
}
