package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UnidadDTO {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private Long proveedorId;
    private String proveedorNombre;
    private String codigoUnico;
    private String codigoBarras;
    private String imei1;
    private String imei2;
    private String condicion;
    private Integer nivelBateria;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private String color;
    private String tipoBaja;
    private String motivoBaja;
    private java.time.LocalDateTime fechaBaja;   // valor de la característica 'color' de la unidad
    private String estado;
    private String notas;
    private Map<String, String> caracteristicas;
    private LocalDateTime createdAt;
}
