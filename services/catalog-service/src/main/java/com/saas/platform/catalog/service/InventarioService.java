package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.AjustarInventarioRequest;
import com.saas.platform.catalog.dto.InventarioDTO;
import com.saas.platform.catalog.dto.MovimientoInventarioDTO;
import com.saas.platform.catalog.entity.*;
import com.saas.platform.catalog.exception.InsufficientStockException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class InventarioService {

    private final InventarioRepository repo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final BodegaRepository bodegaRepo;
    private final UnidadRepository unidadRepo;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<InventarioDTO> getPorBodega(Long bodegaId) {
        return repo.findByBodegaIdConStock(bodegaId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Existencias de una bodega, paginadas; q busca por producto, código, IMEI o código de barras. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<InventarioDTO> paginarPorBodega(Long bodegaId, String q, Integer page, Integer size) {
        String like = q == null || q.isBlank() ? "" : "%" + q.trim().toLowerCase() + "%";
        return com.saas.platform.catalog.dto.PaginaDTO.de(repo.paginarPorBodega(bodegaId, like, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by("unidad.producto.nombre", "id"))), this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<InventarioDTO> getPorProducto(Long productoId) {
        return repo.findByProductoIdConStock(productoId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Integer getStockTotal(Long productoId) {
        Integer s = repo.getStockTotalProducto(productoId);
        return s != null ? s : 0;
    }

    @Transactional
    public InventarioDTO ajustar(AjustarInventarioRequest req, Long usuarioId) {
        Bodega bodega = bodegaRepo.findById(req.getBodegaId())
            .orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada"));
        Unidad unidad = unidadRepo.findById(req.getUnidadId())
            .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada"));

        Inventario inv = repo.findByBodegaIdAndUnidadId(req.getBodegaId(), req.getUnidadId())
            .orElseGet(() -> repo.save(Inventario.builder().bodega(bodega).unidad(unidad).cantidad(0).build()));

        Integer anterior = inv.getCantidad();
        String tipo = req.getTipo() != null ? req.getTipo() : "ajuste";
        Integer nueva;
        switch (tipo) {
            case "entrada": nueva = anterior + req.getCantidad(); break;
            case "salida":
                if (anterior < req.getCantidad())
                    throw new InsufficientStockException("Stock insuficiente. Disponible: " + anterior);
                nueva = anterior - req.getCantidad(); break;
            default: nueva = req.getCantidad();
        }
        inv.setCantidad(nueva);
        Inventario saved = repo.save(inv);

        movimientoRepo.save(MovimientoInventario.builder()
            .inventario(saved)
            .tipo(MovimientoInventario.TipoMovimiento.valueOf(tipo))
            .cantidad(req.getCantidad()).cantidadAnterior(anterior).cantidadNueva(nueva)
            .motivo(req.getMotivo()).usuarioId(usuarioId).build());

        log.info("Inventario ajustado bodega={} unidad={} {}->{}", req.getBodegaId(), req.getUnidadId(), anterior, nueva);
        auditoria.registrar("INVENTARIO_MOVIMIENTO", "inventario", inv.getId(), java.util.Map.of("tipo", tipo, "unidadId", String.valueOf(req.getUnidadId()), "bodegaId", String.valueOf(req.getBodegaId()), "cantidad", String.valueOf(req.getCantidad()), "anterior", anterior, "nueva", nueva, "motivo", String.valueOf(req.getMotivo())));
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> getMovimientos(Long inventarioId) {
        return movimientoRepo.findByInventarioIdOrderByCreatedAtDesc(inventarioId)
            .stream().map(this::toMov).collect(Collectors.toList());
    }

    private InventarioDTO toDTO(Inventario i) {
        return InventarioDTO.builder()
            .id(i.getId())
            .bodegaId(i.getBodega() != null ? i.getBodega().getId() : null)
            .bodegaNombre(i.getBodega() != null ? i.getBodega().getNombre() : null)
            .unidadId(i.getUnidad() != null ? i.getUnidad().getId() : null)
            .unidadCodigo(i.getUnidad() != null ? i.getUnidad().getCodigoUnico() : null)
            .productoNombre(i.getUnidad() != null && i.getUnidad().getProducto() != null ? i.getUnidad().getProducto().getNombre() : null)
            .cantidad(i.getCantidad()).cantidadReservada(i.getCantidadReservada()).disponible(i.getDisponible())
            .ubicacion(i.getUbicacion()).updatedAt(i.getUpdatedAt()).build();
    }

    private MovimientoInventarioDTO toMov(MovimientoInventario m) {
        return MovimientoInventarioDTO.builder()
            .id(m.getId()).inventarioId(m.getInventario() != null ? m.getInventario().getId() : null)
            .ordenId(m.getOrdenId()).tipo(m.getTipo() != null ? m.getTipo().name() : null)
            .cantidad(m.getCantidad()).cantidadAnterior(m.getCantidadAnterior()).cantidadNueva(m.getCantidadNueva())
            .motivo(m.getMotivo()).usuarioId(m.getUsuarioId()).createdAt(m.getCreatedAt()).build();
    }
}
