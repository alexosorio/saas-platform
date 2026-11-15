package com.saas.platform.catalog.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrecioDTO {
    private Long id;
    private Long listaPreciosId;
    private String listaNombre;
    private Long productoId;
    private String productoNombre;
    private BigDecimal precio;
    private BigDecimal margenPorcentaje;
    private LocalDateTime vigenteDesde;
    private LocalDateTime vigenteHasta;
}
