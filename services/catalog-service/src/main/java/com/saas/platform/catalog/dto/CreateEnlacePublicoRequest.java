package com.saas.platform.catalog.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateEnlacePublicoRequest {
    private String nombreEnlace;
    private String descripcion;
    private String password;          // si viene, se hashea y requiere_password=true
    private LocalDateTime fechaExpiracion;
    private Integer maxVistas;
    private Boolean mostrarPrecios;   // default true
    private Boolean mostrarStock;     // default false
    private Boolean permitirBusqueda; // default true
}
