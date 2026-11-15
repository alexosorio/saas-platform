package com.saas.platform.catalog.dto;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CaracteristicaDTO {
    private Long id;
    private String nombre;
    private String codigo;
    private String tipoDato;
    private String opcionesLista;
    private Boolean obligatorio;
    private String aplicableA;
}
