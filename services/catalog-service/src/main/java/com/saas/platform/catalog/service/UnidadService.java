package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CreateUnidadRequest;
import com.saas.platform.catalog.dto.UnidadDTO;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.DuplicateResourceException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class UnidadService {

    private final UnidadRepository repo;
    private final ProductoRepository productoRepo;
    private final com.saas.platform.catalog.repository.ProveedorRepository proveedorRepo;
    private final CaracteristicaProductoRepository caracteristicaRepo;
    private final ValorCaracteristicaRepository valorRepo;
    private final BodegaRepository bodegaRepo;
    private final InventarioRepository inventarioRepo;
    private final CuentaService cuentaService;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<UnidadDTO> getPorProducto(Long productoId) {
        return repo.findByProductoId(productoId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnidadDTO getById(Long id) {
        return toDTO(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada")));
    }

    @Transactional(readOnly = true)
    public UnidadDTO buscarPorImei(String imei) {
        return toDTO(repo.findByImei1OrImei2(imei, imei)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontro unidad con IMEI: " + imei)));
    }

    @Transactional
    public UnidadDTO create(CreateUnidadRequest req) {
        // IMEI: 15 dígitos + dígito de control; distintos entre sí y no repetidos
        req.setImei1(com.saas.platform.catalog.util.ImeiValidator.validar(req.getImei1(), "El IMEI 1"));
        req.setImei2(com.saas.platform.catalog.util.ImeiValidator.validar(req.getImei2(), "El IMEI 2"));
        if (req.getImei1() != null && req.getImei1().equals(req.getImei2()))
            throw new com.saas.platform.catalog.exception.InvalidOperationException("El IMEI 1 y el IMEI 2 no pueden ser iguales");
        for (String imei : new String[]{ req.getImei1(), req.getImei2() })
            if (imei != null && repo.findByImei1OrImei2(imei, imei).isPresent())
                throw new com.saas.platform.catalog.exception.InvalidOperationException("Ya existe una unidad con el IMEI " + imei);

        req.setCodigoBarras(normalizarCodigoBarras(req.getCodigoBarras()));
        if (repo.existsByCodigoUnico(req.getCodigoUnico()))
            throw new DuplicateResourceException("Ya existe unidad con codigo: " + req.getCodigoUnico());

        Producto producto = productoRepo.findById(req.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        com.saas.platform.catalog.entity.Proveedor proveedor = null;
        if (req.getProveedorId() != null) {
            proveedor = proveedorRepo.findById(req.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        }
        Unidad u = Unidad.builder()
            .producto(producto).proveedor(proveedor).codigoUnico(req.getCodigoUnico())
            .codigoBarras(req.getCodigoBarras())
            .imei1(req.getImei1()).imei2(req.getImei2())
            .condicion(req.getCondicion() != null ? Unidad.Condicion.valueOf(req.getCondicion()) : Unidad.Condicion.nuevo)
            .nivelBateria(req.getNivelBateria()).precioCompra(req.getPrecioCompra()).precioVenta(req.getPrecioVenta())
            .estado(Unidad.EstadoUnidad.disponible).notas(req.getNotas()).build();
        Unidad saved = repo.save(u);

        // Caracteristicas dinamicas (EAV)
        if (req.getCaracteristicas() != null) {
            for (Map.Entry<String, String> e : req.getCaracteristicas().entrySet()) {
                caracteristicaRepo.findByCodigo(e.getKey()).ifPresent(carac ->
                    valorRepo.save(ValorCaracteristica.builder()
                        .unidad(saved).caracteristica(carac).valor(e.getValue()).build()));
            }
        }

        // Agregar a inventario si se especifica bodega
        if (req.getBodegaId() != null) {
            Bodega bodega = bodegaRepo.findById(req.getBodegaId())
                .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada"));
            inventarioRepo.save(Inventario.builder().bodega(bodega).unidad(saved).cantidad(1).build());
        }
        cuentaService.crearCxPParaUnidad(saved);

        log.info("Unidad creada: {}", req.getCodigoUnico());
        auditoria.registrar("INVENTARIO_INGRESO", "unidad", saved.getId(), java.util.Map.of("codigo", String.valueOf(saved.getCodigoUnico()), "productoId", String.valueOf(req.getProductoId()), "bodegaId", String.valueOf(req.getBodegaId()), "precioCompra", String.valueOf(req.getPrecioCompra())));
        return getById(saved.getId());
    }

    // Las unidades no se eliminan: ver BajaUnidadService.darDeBaja (salida de inventario + nota crédito + auditoría)

    /** Asigna o cambia (o quita, con vacío) el código de barras de una unidad de la organización. */
    @Transactional
    public UnidadDTO cambiarCodigoBarras(Long id, String codigo, Long orgId) {
        Unidad u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada"));
        if (u.getProducto() == null || !orgId.equals(u.getProducto().getOrganizacionId()))
            throw new ResourceNotFoundException("Unidad no encontrada");
        String anterior = u.getCodigoBarras();
        u.setCodigoBarras(normalizarCodigoBarras(codigo));
        Unidad saved = repo.save(u);
        auditoria.registrar("UNIDAD_CODIGO_BARRAS", "unidad", id, java.util.Map.of(
            "anterior", String.valueOf(anterior), "nuevo", String.valueOf(saved.getCodigoBarras())));
        return toDTO(saved);
    }

    /** Sin espacios; letras, números y - . / (EAN/UPC son solo dígitos; Code128 admite más). Vacío = sin código. */
    public static String normalizarCodigoBarras(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        String c = codigo.trim();
        if (c.length() > 64)
            throw new com.saas.platform.catalog.exception.InvalidOperationException("El código de barras no puede tener más de 64 caracteres");
        if (!c.matches("[A-Za-z0-9\\-./]+"))
            throw new com.saas.platform.catalog.exception.InvalidOperationException("El código de barras solo puede tener letras, números y los signos - . /");
        return c;
    }

    @Transactional
    public UnidadDTO cambiarEstado(Long id, String estado) {
        Unidad u = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada"));
        if ("dado_de_baja".equals(estado) || u.getEstado() == Unidad.EstadoUnidad.dado_de_baja)
            throw new com.saas.platform.catalog.exception.InvalidOperationException(
                "Las bajas se registran con 'Dar de baja' (genera el movimiento de inventario y la nota crédito)");
        u.setEstado(Unidad.EstadoUnidad.valueOf(estado));
        return toDTO(repo.save(u));
    }

    private UnidadDTO toDTO(Unidad u) {
        Map<String, String> caracteristicas = new HashMap<>();
        if (u.getValoresCaracteristicas() != null)
            for (ValorCaracteristica v : u.getValoresCaracteristicas())
                caracteristicas.put(v.getCaracteristica().getCodigo(), v.getValor());

        return UnidadDTO.builder()
            .id(u.getId())
            .productoId(u.getProducto() != null ? u.getProducto().getId() : null)
            .productoNombre(u.getProducto() != null ? u.getProducto().getNombre() : null)
            .proveedorId(u.getProveedor() != null ? u.getProveedor().getId() : null)
            .proveedorNombre(u.getProveedor() != null ? u.getProveedor().getNombre() : null)
            .codigoUnico(u.getCodigoUnico()).codigoBarras(u.getCodigoBarras()).imei1(u.getImei1()).imei2(u.getImei2())
            .condicion(u.getCondicion() != null ? u.getCondicion().name() : null)
            .nivelBateria(u.getNivelBateria()).precioCompra(u.getPrecioCompra()).precioVenta(u.getPrecioVenta())
            .color(caracteristicas.get("color"))
            .tipoBaja(u.getTipoBaja()).motivoBaja(u.getMotivoBaja()).fechaBaja(u.getFechaBaja())
            .estado(u.getEstado() != null ? u.getEstado().name() : null)
            .notas(u.getNotas()).caracteristicas(caracteristicas).createdAt(u.getCreatedAt()).build();
    }
}
