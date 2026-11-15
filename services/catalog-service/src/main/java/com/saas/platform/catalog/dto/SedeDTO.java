package com.saas.platform.catalog.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SedeDTO {
    private Long id; private Long ciudadId; private String ciudadNombre;
    private String nombre; private String direccion; private String telefono; private String email;
    private Boolean activo; private LocalDateTime createdAt;
}
