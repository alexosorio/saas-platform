package com.saas.platform.catalog.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MovimientoInventarioDTO {
    private Long id;
    private Long inventarioId;
    private Long ordenId;
    private String tipo;
    private Integer cantidad;
    private Integer cantidadAnterior;
    private Integer cantidadNueva;
    private String motivo;
    private Long usuarioId;
    private LocalDateTime createdAt;
}
