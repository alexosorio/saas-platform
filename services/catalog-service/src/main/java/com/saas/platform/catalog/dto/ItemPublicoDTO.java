package com.saas.platform.catalog.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ItemPublicoDTO {
    private Long productoId;
    private String nombre;
    private String sku;
    private String descripcion;
    private String imagenUrl;
    private String categoriaNombre;
    private String marcaNombre;
    private BigDecimal precio;          // precio "desde" (mínimo entre unidades disponibles)
    private BigDecimal precioHasta;     // máximo; si difiere de precio se muestra rango
    private java.util.List<String> colores;   // colores disponibles (de las unidades)
    private Integer stockTotal;      // solo si mostrarStock=true
}
