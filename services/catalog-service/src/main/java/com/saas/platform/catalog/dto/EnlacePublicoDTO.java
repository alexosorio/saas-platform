package com.saas.platform.catalog.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EnlacePublicoDTO {
    private Long id;
    private Long listaPreciosId;
    private String token;
    private String nombreEnlace;
    private String descripcion;
    private Boolean requierePassword;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private Integer maxVistas;
    private Integer vistasActuales;
    private Boolean mostrarPrecios;
    private Boolean mostrarStock;
    private Boolean permitirBusqueda;
    private String urlPublica;        // URL completa lista para compartir
}
