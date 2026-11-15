package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.DuplicateResourceException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepo;
    private final MarcaRepository marcaRepo;
    private final InventarioRepository inventarioRepo;
    private final HistorialPrecioProductoRepository historialRepo;
    private final ResumenUnidadesService resumen;
    private final com.saas.platform.catalog.repository.UnidadRepository unidadRepoBarras;

    @Transactional(readOnly = true)
    public List<ProductoDTO> getAll(Long orgId) {
        return enriquecer(repo.findByOrganizacionId(orgId), orgId, null);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> getActivos(Long orgId) {
        return enriquecer(repo.findByOrganizacionIdAndActivoTrue(orgId), orgId, null);
    }

    @Transactional(readOnly = true)
    public ProductoDTO getById(Long id, Long orgId) {
        Producto p = find(id, orgId);
        return enriquecer(java.util.List.of(p), orgId, p.getId()).get(0);
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> search(Long orgId, String q) {
        return enriquecer(repo.searchByOrganizacion(orgId, q), orgId, null);
    }

    /** Productos paginados por nombre; q busca por nombre, SKU o código de barras de sus unidades. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<ProductoDTO> paginado(Long orgId, String q, Boolean activo, Integer page, Integer size) {
        final String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        org.springframework.data.jpa.domain.Specification<Producto> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("organizacionId"), orgId));
            if (activo != null) ps.add(cb.equal(root.get("activo"), activo));
            if (like != null) {
                var sq = query.subquery(Long.class);
                var u = sq.from(com.saas.platform.catalog.entity.Unidad.class);
                sq.select(u.get("id")).where(cb.equal(u.get("producto"), root), cb.like(cb.lower(u.get("codigoBarras")), like));
                ps.add(cb.or(cb.like(cb.lower(root.get("nombre")), like), cb.like(cb.lower(root.get("sku")), like), cb.exists(sq)));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        var pagina = repo.findAll(spec, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by("nombre", "id")));
        return com.saas.platform.catalog.dto.PaginaDTO.de(pagina, enriquecer(pagina.getContent(), orgId, null));
    }

    @Transactional(readOnly = true)
    public ProductoDTO getByBarcode(Long orgId, String codigo) {
        return unidadRepoBarras.findByCodigoBarrasAndProducto_OrganizacionIdOrderByIdAsc(codigo == null ? null : codigo.trim(), orgId)
            .stream().findFirst().map(com.saas.platform.catalog.entity.Unidad::getProducto)
            .map(p -> enriquecer(java.util.List.of(p), orgId, p.getId()).get(0))
            .orElseThrow(() -> new ResourceNotFoundException("No hay producto con codigo de barras: " + codigo));
    }

    @Transactional(readOnly = true)
    public List<HistorialPrecioDTO> getHistorialPrecios(Long productoId, Long orgId) {
        find(productoId, orgId);  // valida pertenencia
        return historialRepo.findByProductoIdOrderByFechaCambioDesc(productoId)
            .stream().map(this::toHistDTO).collect(Collectors.toList());
    }

    @Transactional
    public ProductoDTO create(CreateProductoRequest req, Long orgId, Long usuarioId) {
        if (req.getSku() != null && !req.getSku().isBlank()
                && repo.existsBySkuAndOrganizacionId(req.getSku(), orgId))
            throw new DuplicateResourceException("Ya existe producto con SKU: " + req.getSku());

        Categoria categoria = categoriaRepo.findById(req.getCategoriaId())
            .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        Marca marca = req.getMarcaId() != null
            ? marcaRepo.findById(req.getMarcaId()).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada")) : null;

        boolean requiereImei = req.getRequiereImei() != null
            ? req.getRequiereImei()
            : "movil".equalsIgnoreCase(req.getTipoProducto());

        Producto p = Producto.builder()
            .organizacionId(orgId).nombre(req.getNombre()).sku(req.getSku())
            .descripcion(req.getDescripcion()).imagenUrl(req.getImagenUrl())
            .categoria(categoria).marca(marca)
            .tipoProducto(req.getTipoProducto() != null ? req.getTipoProducto() : "accesorio")
            .requiereImei(requiereImei)
            .activo(true).build();
        Producto saved = repo.save(p);
        log.info("Producto creado: {} (tipo={})", req.getNombre(), p.getTipoProducto());
        return enriquecer(java.util.List.of(saved), orgId, saved.getId()).get(0);
    }

    @Transactional
    public ProductoDTO update(Long id, UpdateProductoRequest req, Long orgId, Long usuarioId) {
        Producto p = find(id, orgId);

        if (req.getNombre() != null) p.setNombre(req.getNombre());
        if (req.getSku() != null) p.setSku(req.getSku());
        if (req.getDescripcion() != null) p.setDescripcion(req.getDescripcion());
        if (req.getImagenUrl() != null) p.setImagenUrl(req.getImagenUrl());
        if (req.getCategoriaId() != null)
            p.setCategoria(categoriaRepo.findById(req.getCategoriaId()).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada")));
        if (req.getMarcaId() != null)
            p.setMarca(marcaRepo.findById(req.getMarcaId()).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada")));
        if (req.getTipoProducto() != null) p.setTipoProducto(req.getTipoProducto());
        if (req.getRequiereImei() != null) p.setRequiereImei(req.getRequiereImei());
        else if (req.getTipoProducto() != null) p.setRequiereImei("movil".equalsIgnoreCase(req.getTipoProducto()));

        Producto saved = repo.save(p);
        return enriquecer(java.util.List.of(saved), orgId, saved.getId()).get(0);
    }

    @Transactional
    public void delete(Long id, Long orgId) {
        Producto p = find(id, orgId);
        repo.delete(p);
    }

    @Transactional
    public ProductoDTO activar(Long id, Long orgId) {
        Producto p = find(id, orgId); p.setActivo(true); Producto g = repo.save(p); return enriquecer(java.util.List.of(g), orgId, g.getId()).get(0);
    }

    @Transactional
    public ProductoDTO desactivar(Long id, Long orgId) {
        Producto p = find(id, orgId); p.setActivo(false); Producto g = repo.save(p); return enriquecer(java.util.List.of(g), orgId, g.getId()).get(0);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Producto find(Long id, Long orgId) {
        Producto p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (!p.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Producto no encontrado");
        return p;
    }


    /** Registra un cambio de precio en la tabla de auditoria. Publico para reuso (bulk). */
    @Transactional
    public void registrarCambioPrecio(Long productoId, BigDecimal ventaAnt, BigDecimal ventaNue,
                                      BigDecimal compraAnt, BigDecimal compraNue, Long usuarioId, String motivo) {
        historialRepo.save(HistorialPrecioProducto.builder()
            .productoId(productoId)
            .precioVentaAnterior(ventaAnt).precioVentaNuevo(ventaNue)
            .precioCompraAnterior(compraAnt).precioCompraNuevo(compraNue)
            .usuarioId(usuarioId).motivo(motivo).build());
    }

    /** Convierte a DTO agregando lo que resume de sus unidades (colores, rango de precio, stock). Una consulta por concepto. */
    private java.util.List<ProductoDTO> enriquecer(java.util.List<Producto> productos, Long orgId, Long productoId) {
        if (productos.isEmpty()) return java.util.List.of();
        var colores = resumen.coloresDisponibles(orgId, productoId);
        var precios = resumen.preciosDisponibles(orgId, productoId);
        var stock = resumen.stockPorProducto(orgId, productoId);
        return productos.stream().map(p -> {
            var unidades = precios.getOrDefault(p.getId(), java.util.List.of());
            var ventas = unidades.stream().map(ResumenUnidadesService.PrecioUnidad::venta)
                .filter(java.util.Objects::nonNull).sorted().toList();
            return toDTO(p).toBuilder()
                .coloresDisponibles(colores.getOrDefault(p.getId(), java.util.List.of()))
                .precioVentaDesde(ventas.isEmpty() ? null : ventas.get(0))
                .precioVentaHasta(ventas.isEmpty() ? null : ventas.get(ventas.size() - 1))
                .unidadesDisponibles(unidades.size())
                .stockTotal(stock.getOrDefault(p.getId(), 0))
                .build();
        }).collect(Collectors.toList());
    }

    private ProductoDTO toDTO(Producto p) {
        return ProductoDTO.builder()
            .id(p.getId()).organizacionId(p.getOrganizacionId())
            .nombre(p.getNombre()).sku(p.getSku()).descripcion(p.getDescripcion()).imagenUrl(p.getImagenUrl())
            .tipoProducto(p.getTipoProducto()).requiereImei(p.getRequiereImei())
            .activo(p.getActivo())
            .categoriaId(p.getCategoria() != null ? p.getCategoria().getId() : null)
            .categoriaNombre(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
            .marcaId(p.getMarca() != null ? p.getMarca().getId() : null)
            .marcaNombre(p.getMarca() != null ? p.getMarca().getNombre() : null)
            .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }

    private HistorialPrecioDTO toHistDTO(HistorialPrecioProducto h) {
        return HistorialPrecioDTO.builder()
            .id(h.getId()).productoId(h.getProductoId())
            .precioVentaAnterior(h.getPrecioVentaAnterior()).precioVentaNuevo(h.getPrecioVentaNuevo())
            .precioCompraAnterior(h.getPrecioCompraAnterior()).precioCompraNuevo(h.getPrecioCompraNuevo())
            .usuarioId(h.getUsuarioId()).motivo(h.getMotivo()).fechaCambio(h.getFechaCambio()).build();
    }
}
