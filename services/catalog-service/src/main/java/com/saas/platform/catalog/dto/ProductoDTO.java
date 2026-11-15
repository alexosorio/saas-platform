package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor
public class ProductoDTO {
    private Long id;
    private Long organizacionId;
    private Long categoriaId;
    private String categoriaNombre;
    private Long marcaId;
    private String marcaNombre;
    private String nombre;
    private String sku;
    private String descripcion;
    private String imagenUrl;
    // Resumen de sus UNIDADES disponibles (los precios viven en la unidad)
    private java.util.List<String> coloresDisponibles;
    private BigDecimal precioVentaDesde;
    private BigDecimal precioVentaHasta;
    private Integer unidadesDisponibles;
    private String tipoProducto;
    private Boolean requiereImei;
    private Boolean activo;
    private Integer stockTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
