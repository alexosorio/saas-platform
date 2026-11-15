package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HistorialPrecioDTO {
    private Long id;
    private Long productoId;
    private BigDecimal precioVentaAnterior;
    private BigDecimal precioVentaNuevo;
    private BigDecimal precioCompraAnterior;
    private BigDecimal precioCompraNuevo;
    private Long usuarioId;
    private String motivo;
    private LocalDateTime fechaCambio;
}
