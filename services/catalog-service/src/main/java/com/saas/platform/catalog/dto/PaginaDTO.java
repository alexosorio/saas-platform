package com.saas.platform.catalog.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Página de resultados para las tablas (paginación en el servidor). Páginas base 0.
 * resumen: totales calculados sobre TODO el filtro (no solo la página), p. ej. el saldo pendiente.
 */
public record PaginaDTO<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas,
                           Map<String, Object> resumen) {

    public static final int TAMANO_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    /** Pageable seguro: página >= 0 y tamaño entre 1 y 100. */
    public static Pageable solicitud(Integer page, Integer size, Sort orden) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? TAMANO_DEFECTO : Math.min(size, TAMANO_MAXIMO);
        return PageRequest.of(p, s, orden == null ? Sort.unsorted() : orden);
    }

    public static <E, T> PaginaDTO<T> de(Page<E> pagina, Function<E, T> mapeo) {
        return de(pagina, pagina.getContent().stream().map(mapeo).toList());
    }

    /** Cuando el contenido se arma aparte (p. ej. enriquecido en lote). */
    public static <T> PaginaDTO<T> de(Page<?> pagina, List<T> contenido) {
        return new PaginaDTO<>(contenido, pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(),
            pagina.getTotalPages(), null);
    }

    public PaginaDTO<T> conResumen(Map<String, Object> r) {
        return new PaginaDTO<>(contenido, pagina, tamano, totalElementos, totalPaginas, r);
    }
}
