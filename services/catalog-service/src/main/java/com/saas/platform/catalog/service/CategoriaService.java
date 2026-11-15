package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CategoriaDTO;
import com.saas.platform.catalog.dto.CreateCategoriaRequest;
import com.saas.platform.catalog.entity.Categoria;
import com.saas.platform.catalog.exception.DuplicateResourceException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class CategoriaService {

    private final CategoriaRepository repo;

    @Transactional(readOnly = true)
    public List<CategoriaDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> getActivas() {
        return repo.findByActivoTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaDTO getById(Long id) {
        return toDTO(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada")));
    }

    @Transactional
    public CategoriaDTO create(CreateCategoriaRequest req) {
        if (repo.existsByCodigo(req.getCodigo()))
            throw new DuplicateResourceException("Ya existe categoria con codigo: " + req.getCodigo());
        Categoria c = Categoria.builder()
            .nombre(req.getNombre()).codigo(req.getCodigo())
            .descripcion(req.getDescripcion()).activo(true).build();
        log.info("Categoria creada: {}", req.getNombre());
        return toDTO(repo.save(c));
    }

    @Transactional
    public CategoriaDTO update(Long id, CreateCategoriaRequest req) {
        Categoria c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (!req.getCodigo().equals(c.getCodigo()) && repo.existsByCodigo(req.getCodigo()))
            throw new DuplicateResourceException("Ya existe categoria con codigo: " + req.getCodigo());
        c.setNombre(req.getNombre());
        c.setCodigo(req.getCodigo());
        c.setDescripcion(req.getDescripcion());
        return toDTO(repo.save(c));
    }

    @Transactional
    public void delete(Long id) {
        Categoria c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        repo.delete(c);
    }

    @Transactional
    public CategoriaDTO activar(Long id) {
        Categoria c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        c.setActivo(true); return toDTO(repo.save(c));
    }

    @Transactional
    public CategoriaDTO desactivar(Long id) {
        Categoria c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        c.setActivo(false); return toDTO(repo.save(c));
    }

    private CategoriaDTO toDTO(Categoria c) {
        return CategoriaDTO.builder()
            .id(c.getId()).nombre(c.getNombre()).codigo(c.getCodigo())
            .descripcion(c.getDescripcion()).activo(c.getActivo()).createdAt(c.getCreatedAt()).build();
    }
}
