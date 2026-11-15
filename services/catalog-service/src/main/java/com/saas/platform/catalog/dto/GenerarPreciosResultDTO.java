package com.saas.platform.catalog.dto;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GenerarPreciosResultDTO {
    private int creados;            // cuantos precios nuevos se crearon
    private int actualizados;       // cuantos se sobrescribieron (si sobrescribir=true)
    private int omitidos;           // ya tenian precio y sobrescribir=false
    private int sinPrecioCompra;    // productos sin unidades con precio_compra (MARGEN_SOBRE_COMPRA)
    private int sinPrecioEnOrigen;  // productos sin precio en lista origen (AJUSTE_SOBRE_LISTA)
    private List<String> mensajes;  // mensajes de error/advertencia
}
