package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CaracteristicaDTO;
import com.saas.platform.catalog.dto.CreateCaracteristicaRequest;
import com.saas.platform.catalog.entity.CaracteristicaProducto;
import com.saas.platform.catalog.exception.DuplicateResourceException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.CaracteristicaProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class CaracteristicaService {

    private final CaracteristicaProductoRepository repo;

    @Transactional(readOnly = true)
    public List<CaracteristicaDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CaracteristicaDTO> getPorTipo(String aplicableA) {
        return repo.findByAplicableA(CaracteristicaProducto.AplicableA.valueOf(aplicableA))
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public CaracteristicaDTO create(CreateCaracteristicaRequest req) {
        if (repo.existsByCodigo(req.getCodigo()))
            throw new DuplicateResourceException("Ya existe caracteristica con codigo: " + req.getCodigo());
        CaracteristicaProducto c = CaracteristicaProducto.builder()
            .nombre(req.getNombre()).codigo(req.getCodigo())
            .tipoDato(req.getTipoDato() != null ? CaracteristicaProducto.TipoDato.valueOf(req.getTipoDato()) : CaracteristicaProducto.TipoDato.texto)
            .opcionesLista(req.getOpcionesLista())
            .obligatorio(req.getObligatorio() != null ? req.getObligatorio() : false)
            .aplicableA(req.getAplicableA() != null ? CaracteristicaProducto.AplicableA.valueOf(req.getAplicableA()) : CaracteristicaProducto.AplicableA.todos)
            .build();
        log.info("Caracteristica creada: {}", req.getNombre());
        return toDTO(repo.save(c));
    }

    @Transactional
    public CaracteristicaDTO update(Long id, CreateCaracteristicaRequest req) {
        CaracteristicaProducto c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Caracteristica no encontrada"));
        c.setNombre(req.getNombre());
        c.setOpcionesLista(req.getOpcionesLista());
        if (req.getObligatorio() != null) c.setObligatorio(req.getObligatorio());
        if (req.getTipoDato() != null) c.setTipoDato(CaracteristicaProducto.TipoDato.valueOf(req.getTipoDato()));
        if (req.getAplicableA() != null) c.setAplicableA(CaracteristicaProducto.AplicableA.valueOf(req.getAplicableA()));
        return toDTO(repo.save(c));
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Caracteristica no encontrada")));
    }

    private CaracteristicaDTO toDTO(CaracteristicaProducto c) {
        return CaracteristicaDTO.builder()
            .id(c.getId()).nombre(c.getNombre()).codigo(c.getCodigo())
            .tipoDato(c.getTipoDato() != null ? c.getTipoDato().name() : null)
            .opcionesLista(c.getOpcionesLista()).obligatorio(c.getObligatorio())
            .aplicableA(c.getAplicableA() != null ? c.getAplicableA().name() : null).build();
    }
}
