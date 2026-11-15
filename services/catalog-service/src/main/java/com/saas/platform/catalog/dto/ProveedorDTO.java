package com.saas.platform.catalog.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProveedorDTO {
    private Long id;
    private Long organizacionId;
    private String nombre;
    private String identificacion;
    private String telefono;
    private String email;
    private String direccion;
    private String contacto;
    private String notas;
    private Boolean activo;
    private LocalDateTime createdAt;
}
