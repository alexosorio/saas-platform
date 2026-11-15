package com.saas.platform.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuracion para generar precios en masa para una lista.
 *
 * Tres estrategias:
 *   - MARGEN_SOBRE_COMPRA:  precio = precio_compra * (1 + valor/100)
 *     Necesita: valor (porcentaje de margen). Usa el precio_compra mas
 *     reciente de las unidades del producto.
 *
 *   - AJUSTE_SOBRE_LISTA:   precio = precio_otra_lista * (1 + valor/100)
 *     Necesita: valor (puede ser negativo para descuento), listaOrigenId.
 *     Ej: 'lista mayorista = lista minorista - 15%' -> valor = -15, lista_origen = minorista
 *
 *   - VALOR_FIJO:           precio = valor
 *     Necesita: valor.
 *
 * Filtros (opcionales):
 *   - categoriaIds / marcaIds:    si vacios, aplica a todos los productos activos
 *                                  de la organizacion.
 *   - productoIds:                si especificas estos, ignora los filtros de
 *                                  categoria/marca y solo aplica a esos productos.
 *   - sobrescribir:               si false (default), solo crea precios para productos
 *                                  que NO los tienen ya en la lista. Si true, cierra
 *                                  el precio vigente y crea uno nuevo (histórico).
 */
@Data
public class GenerarPreciosRequest {

    @NotNull
    private Estrategia estrategia;

    @NotNull
    private BigDecimal valor;            // porcentaje o valor fijo, segun estrategia

    private Long listaOrigenId;          // solo para AJUSTE_SOBRE_LISTA

    private List<Long> categoriaIds;     // filtro opcional
    private List<Long> marcaIds;         // filtro opcional
    private List<Long> productoIds;      // si se especifica, gana sobre filtros

    private Boolean sobrescribir = false;

    public enum Estrategia {
        MARGEN_SOBRE_COMPRA,
        AJUSTE_SOBRE_LISTA,
        VALOR_FIJO
    }
}
