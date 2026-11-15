package com.saas.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String nombre;
    private String apellido;
    private String telefono;
    private String avatarUrl;
    private String idioma;
    private String zonaHoraria;
    private Long rolId;
    private Boolean activo;
}
