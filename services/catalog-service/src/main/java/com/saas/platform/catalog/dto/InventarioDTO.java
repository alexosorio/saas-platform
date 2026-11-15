package com.saas.platform.catalog.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InventarioDTO {
    private Long id;
    private Long bodegaId;
    private String bodegaNombre;
    private Long unidadId;
    private String unidadCodigo;
    private String productoNombre;
    private Integer cantidad;
    private Integer cantidadReservada;
    private Integer disponible;
    private String ubicacion;
    private LocalDateTime updatedAt;
}
