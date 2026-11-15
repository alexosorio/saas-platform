package com.saas.platform.catalog.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CategoriaDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime createdAt;
}
