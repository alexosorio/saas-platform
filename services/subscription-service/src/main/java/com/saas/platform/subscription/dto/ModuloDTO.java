package com.saas.platform.subscription.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ModuloDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String descripcion;
    private String ruta;
    private String icono;
    private Integer orden;
}
