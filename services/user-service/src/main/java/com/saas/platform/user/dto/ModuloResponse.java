package com.saas.platform.user.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ModuloResponse {
    private Long id;
    private String nombre;
    private String codigo;
    private String descripcion;
}
