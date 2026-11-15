package com.saas.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String email;
    private String nombre;     // del perfil
    private String apellido;   // del perfil
    private Boolean emailVerificado;
    private String rol;
    private String message;
}
