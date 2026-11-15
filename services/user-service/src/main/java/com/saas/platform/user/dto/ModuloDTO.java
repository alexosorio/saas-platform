package com.saas.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuloDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String ruta;
    private String icono;
    private Boolean crear;
    private Boolean leer;
    private Boolean actualizar;
    private Boolean eliminar;
}
