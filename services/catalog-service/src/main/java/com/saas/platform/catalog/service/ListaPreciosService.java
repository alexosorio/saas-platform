package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.*;
import com.saas.platform.catalog.entity.ListaPrecios;
import com.saas.platform.catalog.entity.Precio;
import com.saas.platform.catalog.entity.Producto;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.ListaPreciosRepository;
import com.saas.platform.catalog.repository.PrecioRepository;
import com.saas.platform.catalog.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class ListaPreciosService {

    private final ListaPreciosRepository repo;
    private final PrecioRepository precioRepo;
    private final ProductoRepository productoRepo;

    @Transactional(readOnly = true)
    public List<ListaPreciosDTO> getAll(Long orgId) {
        return repo.findByOrganizacionId(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListaPreciosDTO> getActivas(Long orgId) {
        return repo.findByOrganizacionIdAndActivaTrue(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListaPreciosDTO getById(Long id) {
        return toDTO(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada")));
    }

    @Transactional
    public ListaPreciosDTO create(CreateListaPreciosRequest req, Long orgId) {
        ListaPrecios l = ListaPrecios.builder()
            .organizacionId(orgId).nombre(req.getNombre())
            .tipoCliente(ListaPrecios.TipoCliente.valueOf(req.getTipoCliente()))
            .descripcion(req.getDescripcion())
            .baseCalculo(req.getBaseCalculo() != null
                ? ListaPrecios.BaseCalculo.valueOf(req.getBaseCalculo()) : ListaPrecios.BaseCalculo.precio_venta)
            .tipoAjuste(req.getTipoAjuste() != null
                ? ListaPrecios.TipoAjuste.valueOf(req.getTipoAjuste()) : ListaPrecios.TipoAjuste.margen_porcentaje)
            .valorAjuste(req.getValorAjuste() != null ? req.getValorAjuste() : java.math.BigDecimal.ZERO)
            .activa(true).build();
        log.info("Lista creada: {} (base={} ajuste={} valor={})",
                 req.getNombre(), l.getBaseCalculo(), l.getTipoAjuste(), l.getValorAjuste());
        return toDTO(repo.save(l));
    }

    @Transactional
    public ListaPreciosDTO update(Long id, CreateListaPreciosRequest req) {
        ListaPrecios l = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        l.setNombre(req.getNombre());
        l.setTipoCliente(ListaPrecios.TipoCliente.valueOf(req.getTipoCliente()));
        l.setDescripcion(req.getDescripcion());
        if (req.getBaseCalculo() != null) l.setBaseCalculo(ListaPrecios.BaseCalculo.valueOf(req.getBaseCalculo()));
        if (req.getTipoAjuste() != null) l.setTipoAjuste(ListaPrecios.TipoAjuste.valueOf(req.getTipoAjuste()));
        if (req.getValorAjuste() != null) l.setValorAjuste(req.getValorAjuste());
        return toDTO(repo.save(l));
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    @Transactional
    public ListaPreciosDTO activar(Long id) {
        ListaPrecios l = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        l.setActiva(true); return toDTO(repo.save(l));
    }

    @Transactional
    public ListaPreciosDTO desactivar(Long id) {
        ListaPrecios l = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        l.setActiva(false); return toDTO(repo.save(l));
    }

    // Precios

    @Transactional(readOnly = true)
    public List<PrecioDTO> getPrecios(Long listaId) {
        return precioRepo.findByListaPreciosId(listaId).stream().map(this::toPrecioDTO).collect(Collectors.toList());
    }

    @Transactional
    public PrecioDTO agregarPrecio(Long listaId, CreatePrecioRequest req) {
        ListaPrecios lista = repo.findById(listaId).orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        Producto producto = productoRepo.findById(req.getProductoId())
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Cerrar precio anterior vigente
        precioRepo.findPrecioVigente(lista.getId(), producto.getId(), LocalDateTime.now())
            .ifPresent(p -> { p.setVigenteHasta(LocalDateTime.now()); precioRepo.save(p); });

        Precio precio = Precio.builder()
            .listaPrecios(lista).producto(producto)
            .precio(req.getPrecio()).margenPorcentaje(req.getMargenPorcentaje()).build();
        log.info("Precio agregado lista={} producto={}", listaId, producto.getId());
        return toPrecioDTO(precioRepo.save(precio));
    }

    @Transactional
    /** Borra TODOS los overrides manuales de una lista para que aplique la formula. Org-scoped. */
    @org.springframework.transaction.annotation.Transactional
    public int limpiarOverrides(Long listaId, Long orgId) {
        ListaPrecios lista = repo.findById(listaId)
            .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada"));
        if (!lista.getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Lista no encontrada");
        long n = precioRepo.deleteByListaPreciosId(listaId);
        log.info("Overrides eliminados de lista {}: {}", listaId, n);
        return (int) n;
    }

    public void eliminarPrecio(Long precioId) { precioRepo.deleteById(precioId); }

    private ListaPreciosDTO toDTO(ListaPrecios l) {
        return ListaPreciosDTO.builder()
            .id(l.getId()).organizacionId(l.getOrganizacionId()).nombre(l.getNombre())
            .tipoCliente(l.getTipoCliente() != null ? l.getTipoCliente().name() : null)
            .descripcion(l.getDescripcion())
            .baseCalculo(l.getBaseCalculo() != null ? l.getBaseCalculo().name() : null)
            .tipoAjuste(l.getTipoAjuste() != null ? l.getTipoAjuste().name() : null)
            .valorAjuste(l.getValorAjuste())
            .activa(l.getActiva()).createdAt(l.getCreatedAt()).build();
    }

    private PrecioDTO toPrecioDTO(Precio p) {
        return PrecioDTO.builder()
            .id(p.getId())
            .listaPreciosId(p.getListaPrecios() != null ? p.getListaPrecios().getId() : null)
            .listaNombre(p.getListaPrecios() != null ? p.getListaPrecios().getNombre() : null)
            .productoId(p.getProducto() != null ? p.getProducto().getId() : null)
            .productoNombre(p.getProducto() != null ? p.getProducto().getNombre() : null)
            .precio(p.getPrecio()).margenPorcentaje(p.getMargenPorcentaje())
            .vigenteDesde(p.getVigenteDesde()).vigenteHasta(p.getVigenteHasta()).build();
    }
}
