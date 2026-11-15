package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.ScanResultDTO;
import com.saas.platform.catalog.entity.Precio;
import com.saas.platform.catalog.entity.Producto;
import com.saas.platform.catalog.entity.Unidad;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.InventarioRepository;
import com.saas.platform.catalog.repository.PrecioRepository;
import com.saas.platform.catalog.repository.ProductoRepository;
import com.saas.platform.catalog.repository.ListaPreciosRepository;
import com.saas.platform.catalog.repository.UnidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve un codigo (de barras / SKU / IMEI / codigoUnico) a un producto/unidad.
 * Usado por la pantalla de ordenes: el cajero escanea el codigo, este servicio
 * devuelve toda la informacion necesaria para agregar el item a la orden.
 *
 * Orden de busqueda:
 *   1) Unidad por codigoUnico exacto
 *   2) Unidad por imei1 o imei2 exactos
 *   3) Producto por SKU exacto (la unidad disponible se elige despues)
 */
@Service
@RequiredArgsConstructor
public class ScanService {

    private final UnidadRepository unidadRepo;
    private final ProductoRepository productoRepo;
    private final PrecioRepository precioRepo;
    private final InventarioRepository inventarioRepo;
    private final ListaPreciosRepository listaRepo;
    private final PrecioCalculadoService precioCalculadoService;
    private final ResumenUnidadesService resumen;
    private final BodegaAccesoService accesoBodegas;

    @Transactional(readOnly = true)
    public ScanResultDTO scan(String codigo, Long listaPreciosId, Long orgId) {
        return scan(codigo, listaPreciosId, null, java.util.List.of(), null, orgId);
    }

    /**
     * color   : si se busca por producto (SKU / código de barras), toma una unidad de ese color.
     * excluir : unidades ya agregadas a la orden, para poder vender varias del mismo producto.
     * sedeId  : sede de la orden; se prefieren unidades que estén en ella.
     */
    @Transactional(readOnly = true)
    public ScanResultDTO scan(String codigo, Long listaPreciosId, String color, java.util.List<Long> excluir, Long sedeId, Long orgId) {
        if (codigo == null || codigo.isBlank())
            throw new ResourceNotFoundException("Codigo vacio");
        String c = codigo.trim();

        // 1. Unidad por codigoUnico
        Optional<Unidad> u = unidadRepo.findByCodigoUnico(c);
        if (u.isPresent()) { accesoBodegas.validarVenta(u.get()); return conUbicacion(buildResult(u.get(), u.get().getProducto(), listaPreciosId, orgId), sedeId); }

        // 2. Unidad por IMEI
        u = unidadRepo.findByImei1OrImei2(c, c);
        if (u.isPresent()) { accesoBodegas.validarVenta(u.get()); return conUbicacion(buildResult(u.get(), u.get().getProducto(), listaPreciosId, orgId), sedeId); }

        // 3. Producto por SKU
        Optional<Producto> p = productoRepo.findBySkuAndOrganizacionId(c, orgId);
        if (p.isPresent()) {
            // Buscar la primera unidad disponible del producto
            List<Unidad> disponibles = unidadRepo.findByProductoIdAndEstado(p.get().getId(), Unidad.EstadoUnidad.disponible);
            return conUbicacion(resultadoPorProducto(elegirUnidad(disponibles, color, excluir, sedeId), p.get(), color, listaPreciosId, orgId), sedeId);
        }

        // 4. CÓDIGO DE BARRAS de la UNIDAD (el del empaque). Si es el EAN del modelo, varias unidades lo comparten.
        List<Unidad> conCodigo = unidadRepo.findByCodigoBarrasAndProducto_OrganizacionIdOrderByIdAsc(c, orgId);
        if (conCodigo.size() == 1) {
            // Código propio de una sola unidad: se trata igual que escanear su IMEI
            Unidad unica = conCodigo.get(0);
            accesoBodegas.validarVenta(unica);
            return conUbicacion(buildResult(unica, unica.getProducto(), listaPreciosId, orgId), sedeId);
        }
        if (!conCodigo.isEmpty()) return porCodigoCompartido(conCodigo, color, excluir, sedeId, listaPreciosId, orgId);

        throw new ResourceNotFoundException("No se encontro nada con el codigo: " + c);
    }


    /**
     * Primera unidad disponible no incluida aún en la orden y, si se pidió, del color indicado.
     * Si hay varias, se prefiere una que esté en la sede de la orden.
     */
    private Unidad elegirUnidad(List<Unidad> disponibles, String color, java.util.List<Long> excluir, Long sedeId) {
        String buscado = (color == null || color.isBlank()) ? null : ResumenUnidadesService.normalizarColor(color);
        java.util.Set<Long> permitidas = accesoBodegas.bodegasPermitidas();
        return disponibles.stream()
            .filter(u -> excluir == null || !excluir.contains(u.getId()))
            .filter(u -> accesoBodegas.disponibleEn(u.getId(), permitidas))   // solo de las bodegas del usuario
            .filter(u -> buscado == null || buscado.equalsIgnoreCase(colorDe(u)))
            .min(java.util.Comparator.comparingInt((Unidad u) -> enSede(u, sedeId) ? 0 : 1))
            .orElse(null);
    }

    private boolean enSede(Unidad u, Long sedeId) {
        return sedeId != null && accesoBodegas.ubicacion(u.getId(), sedeId)
            .map(i -> sedeId.equals(i.getBodega().getSedeId())).orElse(false);
    }

    /** Completa en el resultado la bodega y sede donde está la unidad elegida. */
    private ScanResultDTO conUbicacion(ScanResultDTO r, Long sedeId) {
        if (r.getUnidadId() == null) return r;
        accesoBodegas.ubicacion(r.getUnidadId(), sedeId).ifPresent(i -> {
            r.setBodegaId(i.getBodega().getId());
            r.setBodegaNombre(i.getBodega().getNombre());
            r.setSedeId(i.getBodega().getSedeId());
            r.setSedeNombre(accesoBodegas.nombreSede(i.getBodega().getSedeId()));
        });
        return r;
    }

    private ScanResultDTO resultadoPorProducto(Unidad elegida, Producto p, String color, Long listaPreciosId, Long orgId) {
        if (elegida == null && color != null && !color.isBlank())
            throw new ResourceNotFoundException("No hay más unidades disponibles de \"" + p.getNombre() + "\" en color " + color);
        ScanResultDTO r = buildResult(elegida, p, listaPreciosId, orgId);
        r.setColoresDisponibles(resumen.coloresDisponibles(orgId, p.getId()).getOrDefault(p.getId(), java.util.List.of()));
        return r;
    }

    /**
     * Código de barras compartido por varias unidades (EAN del modelo): se elige una disponible.
     * Los colores a escoger son solo los de esas unidades (un EAN suele identificar también el color).
     */
    private ScanResultDTO porCodigoCompartido(List<Unidad> conCodigo, String color, java.util.List<Long> excluir,
                                             Long sedeId, Long listaPreciosId, Long orgId) {
        Producto producto = conCodigo.get(0).getProducto();
        java.util.Set<Long> permitidas = accesoBodegas.bodegasPermitidas();
        List<Unidad> disponibles = conCodigo.stream()
            .filter(u -> u.getEstado() == Unidad.EstadoUnidad.disponible)
            .filter(u -> excluir == null || !excluir.contains(u.getId()))
            .filter(u -> accesoBodegas.disponibleEn(u.getId(), permitidas))
            .toList();
        List<String> colores = disponibles.stream().map(this::colorDe).filter(java.util.Objects::nonNull)
            .distinct().sorted().toList();
        String buscado = (color == null || color.isBlank()) ? null : ResumenUnidadesService.normalizarColor(color);
        if (buscado != null && disponibles.stream().noneMatch(u -> buscado.equalsIgnoreCase(colorDe(u))))
            throw new ResourceNotFoundException("No hay más unidades disponibles de \"" + producto.getNombre() + "\" en color " + color);
        Unidad elegida = disponibles.stream()
            .filter(u -> buscado == null || buscado.equalsIgnoreCase(colorDe(u)))
            .min(java.util.Comparator.comparingInt((Unidad u) -> enSede(u, sedeId) ? 0 : 1))
            .orElse(null);
        ScanResultDTO r = buildResult(elegida, producto, listaPreciosId, orgId);
        r.setColoresDisponibles(colores);
        return conUbicacion(r, sedeId);
    }

    private String colorDe(Unidad u) {
        if (u.getValoresCaracteristicas() == null) return null;
        return u.getValoresCaracteristicas().stream()
            .filter(v -> v.getCaracteristica() != null && "color".equalsIgnoreCase(v.getCaracteristica().getCodigo()))
            .map(v -> ResumenUnidadesService.normalizarColor(v.getValor()))
            .findFirst().orElse(null);
    }

    private ScanResultDTO buildResult(Unidad u, Producto p, Long listaPreciosId, Long orgId) {
        // Verificar que el producto pertenece a la org del usuario
        if (p == null || !p.getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Producto fuera de tu organizacion");

        ScanResultDTO.ScanResultDTOBuilder b = ScanResultDTO.builder()
            .productoId(p.getId()).productoNombre(p.getNombre()).productoSku(p.getSku());

        if (u != null) {
            b.tipoEncontrado("unidad")
             .unidadId(u.getId()).unidadCodigo(u.getCodigoUnico()).unidadImei1(u.getImei1())
             .estado(u.getEstado() != null ? u.getEstado().name() : null)
             .precioCompra(u.getPrecioCompra()).precioVenta(u.getPrecioVenta())
             .color(colorDe(u));
        } else {
            b.tipoEncontrado("producto");
        }

        // Stock total del producto
        Integer stock = inventarioRepo.getStockTotalProducto(p.getId());
        b.stockTotal(stock != null ? stock : 0);

        // Si vino una listaPreciosId, calcular el precio segun la formula de la lista
        // El precio sale de la UNIDAD encontrada (sin unidad disponible no hay precio que sugerir)
        if (listaPreciosId != null && u != null) {
            listaRepo.findById(listaPreciosId).ifPresent(lista -> {
                java.math.BigDecimal precio = precioCalculadoService.calcularUnidad(lista, u);
                if (precio != null) b.precioSugerido(precio).listaPreciosId(listaPreciosId);
            });
        }

        return b.build();
    }
}
