package com.saas.platform.catalog.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Resultado de escanear un codigo (barras, SKU, IMEI, codigo unico).
 * Devuelve la informacion necesaria para agregar el item a una orden.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScanResultDTO {
    private String tipoEncontrado;  // "unidad" si se encontro por imei/codigoUnico, "producto" si fue por SKU
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private Long unidadId;
    private String unidadCodigo;
    private String unidadImei1;
    private String estado;          // estado de la unidad (disponible, vendido, etc)
    private Integer stockTotal;     // total de unidades disponibles del producto
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private String color;           // color de la unidad (característica 'color')
    private java.util.List<String> coloresDisponibles;  // si se buscó por producto: colores entre los que elegir
    private BigDecimal precioSugerido;   // si se pidio una listaPreciosId, viene el precio vigente
    private Long listaPreciosId;
    // Dónde está la unidad: si es otra sede, la orden puede cambiarse a ella (con permiso)
    private Long bodegaId;
    private String bodegaNombre;
    private Long sedeId;
    private String sedeNombre;
}
