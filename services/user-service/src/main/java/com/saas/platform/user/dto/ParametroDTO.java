package com.saas.platform.user.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ParametroDTO {
    private String clave;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String tipo;
    private String alcance;            // plataforma | organizacion
    private String valorGeneral;       // valor por defecto de la plataforma
    private String valorOrganizacion;  // valor propio de la organización (null si usa el general)
    private String valorEfectivo;      // el que realmente aplica
    private BigDecimal minimo;
    private BigDecimal maximo;
    private Boolean editable;          // ¿quien consulta puede cambiarlo?
}
