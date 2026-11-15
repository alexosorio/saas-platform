package com.saas.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private Boolean activo;
    private Boolean emailVerificado;
    private String nombre;
    private String apellido;
    private String telefono;
    private String avatarUrl;
    private String idioma;
    private String zonaHoraria;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime createdAt;
    private Long organizacionId;
    private RolInfo rol;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RolInfo {
        private Long id;
        private String nombre;
    }
}
