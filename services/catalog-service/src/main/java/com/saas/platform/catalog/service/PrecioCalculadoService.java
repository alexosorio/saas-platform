package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.PrecioCalculadoDTO;
import com.saas.platform.catalog.entity.ListaPrecios;
import com.saas.platform.catalog.entity.Precio;
import com.saas.platform.catalog.entity.Producto;
import com.saas.platform.catalog.entity.Unidad;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.ListaPreciosRepository;
import com.saas.platform.catalog.repository.PrecioRepository;
import com.saas.platform.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Precios según lista. Los precios base viven en la UNIDAD (el producto es la entidad padre):
 *
 *   base = (lista.baseCalculo == precio_venta) ? unidad.precioVenta : unidad.precioCompra
 *   margen_porcentaje -> base * (1 + valorAjuste/100)
 *   valor_fijo        -> base + valorAjuste
 *
 * Un precio manual (tabla precios) para el PRODUCTO en la lista tiene prioridad sobre la fórmula
 * y aplica a todas sus unidades.
 *
 * Por producto se informa el RANGO (mínimo–máximo) entre sus unidades disponibles, porque cada
 * unidad puede tener costo/precio distinto; precioCalculado = mínimo ("desde").
 */
@Service
@RequiredArgsConstructor
public class PrecioCalculadoService {

    private final ListaPreciosRepository listaRepo;
    private final ProductoRepository productoRepo;
    private final PrecioRepository precioRepo;
    private final ResumenUnidadesService resumen;

    @Transactional(readOnly = true)
    public List<PrecioCalculadoDTO> calcularParaLista(Long listaId, Long orgId) {
        ListaPrecios lista = listaRepo.findById(listaId)
            .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        if (!lista.getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Lista no encontrada");

        List<Producto> productos = productoRepo.findByOrganizacionIdAndActivoTrue(orgId);

        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, Precio> overrides = precioRepo.findByListaPreciosId(listaId).stream()
            .filter(p -> p.getVigenteHasta() == null || p.getVigenteHasta().isAfter(ahora))
            .collect(Collectors.toMap(p -> p.getProducto().getId(), p -> p, (a, b) -> b));

        // Una consulta por concepto para todos los productos
        Map<Long, List<ResumenUnidadesService.PrecioUnidad>> precios = resumen.preciosDisponibles(orgId, null);
        Map<Long, List<String>> colores = resumen.coloresDisponibles(orgId, null);
        Map<Long, Integer> stock = resumen.stockPorProducto(orgId, null);

        return productos.stream().map(p -> {
            List<ResumenUnidadesService.PrecioUnidad> unidades = precios.getOrDefault(p.getId(), List.of());
            BigDecimal min, max;
            boolean esOverride = false;
            Precio override = overrides.get(p.getId());
            if (override != null) {
                min = max = override.getPrecio();
                esOverride = true;
            } else {
                List<BigDecimal> calculados = unidades.stream()
                    .map(u -> aplicarFormula(lista, u.venta(), u.compra()))
                    .filter(Objects::nonNull).sorted().toList();
                min = calculados.isEmpty() ? null : calculados.get(0);
                max = calculados.isEmpty() ? null : calculados.get(calculados.size() - 1);
            }
            return PrecioCalculadoDTO.builder()
                .productoId(p.getId()).nombre(p.getNombre()).sku(p.getSku())
                .marcaNombre(p.getMarca() != null ? p.getMarca().getNombre() : null)
                .categoriaNombre(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                .imagenUrl(p.getImagenUrl())
                .precioCalculado(escala(min)).precioMinimo(escala(min)).precioMaximo(escala(max))
                .esOverride(esOverride)
                .stockTotal(stock.getOrDefault(p.getId(), 0))
                .unidadesDisponibles(unidades.size())
                .colores(colores.getOrDefault(p.getId(), List.of()))
                .build();
        }).collect(Collectors.toList());
    }

    /** Precio de UNA unidad en UNA lista (escaneo / órdenes). */
    @Transactional(readOnly = true)
    public BigDecimal calcularUnidad(ListaPrecios lista, Unidad unidad) {
        Optional<Precio> override = precioRepo.findPrecioVigente(lista.getId(), unidad.getProducto().getId(), LocalDateTime.now());
        if (override.isPresent()) return escala(override.get().getPrecio());
        return escala(aplicarFormula(lista, unidad.getPrecioVenta(), unidad.getPrecioCompra()));
    }

    private BigDecimal aplicarFormula(ListaPrecios lista, BigDecimal venta, BigDecimal compra) {
        BigDecimal base = (lista.getBaseCalculo() == ListaPrecios.BaseCalculo.precio_venta) ? venta : compra;
        if (base == null) return null;
        BigDecimal ajuste = lista.getValorAjuste() != null ? lista.getValorAjuste() : BigDecimal.ZERO;
        if (lista.getTipoAjuste() == ListaPrecios.TipoAjuste.valor_fijo) return base.add(ajuste);
        BigDecimal factor = BigDecimal.ONE.add(ajuste.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return base.multiply(factor);
    }

    private static BigDecimal escala(BigDecimal v) { return v != null ? v.setScale(2, RoundingMode.HALF_UP) : null; }
}
