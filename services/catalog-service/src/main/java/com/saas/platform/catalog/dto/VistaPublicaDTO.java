package com.saas.platform.catalog.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VistaPublicaDTO {
    private Long organizacionId;
    private String organizacionNombre;
    private Boolean tieneLogo;
    private String nombreLista;
    private String tipoCliente;
    private String descripcion;
    private String nombreEnlace;
    private Boolean mostrarPrecios;
    private Boolean mostrarStock;
    private Boolean permitirBusqueda;
    private List<ItemPublicoDTO> items;
}
