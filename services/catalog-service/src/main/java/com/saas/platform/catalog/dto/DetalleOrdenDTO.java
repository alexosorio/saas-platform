package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DetalleOrdenDTO {
    private Long id;
    private Long unidadId;
    private String unidadCodigo;
    private String productoNombre;
    private String color;   // color de la unidad vendida
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuentoLinea;
    private BigDecimal subtotal;
}
