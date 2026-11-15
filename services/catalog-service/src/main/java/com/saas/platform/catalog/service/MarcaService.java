package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CreateMarcaRequest;
import com.saas.platform.catalog.dto.MarcaDTO;
import com.saas.platform.catalog.entity.Marca;
import com.saas.platform.catalog.exception.DuplicateResourceException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class MarcaService {

    private final MarcaRepository repo;

    @Transactional(readOnly = true)
    public List<MarcaDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarcaDTO> getActivas() {
        return repo.findByActivoTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MarcaDTO getById(Long id) {
        return toDTO(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada")));
    }

    @Transactional
    public MarcaDTO create(CreateMarcaRequest req) {
        if (repo.existsByCodigo(req.getCodigo()))
            throw new DuplicateResourceException("Ya existe marca con codigo: " + req.getCodigo());
        Marca m = Marca.builder().nombre(req.getNombre()).codigo(req.getCodigo())
            .logoUrl(req.getLogoUrl()).activo(true).build();
        log.info("Marca creada: {}", req.getNombre());
        return toDTO(repo.save(m));
    }

    @Transactional
    public MarcaDTO update(Long id, CreateMarcaRequest req) {
        Marca m = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        m.setNombre(req.getNombre()); m.setCodigo(req.getCodigo()); m.setLogoUrl(req.getLogoUrl());
        return toDTO(repo.save(m));
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada")));
    }

    @Transactional
    public MarcaDTO activar(Long id) {
        Marca m = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        m.setActivo(true); return toDTO(repo.save(m));
    }

    @Transactional
    public MarcaDTO desactivar(Long id) {
        Marca m = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        m.setActivo(false); return toDTO(repo.save(m));
    }

    private MarcaDTO toDTO(Marca m) {
        return MarcaDTO.builder().id(m.getId()).nombre(m.getNombre()).codigo(m.getCodigo())
            .logoUrl(m.getLogoUrl()).activo(m.getActivo()).createdAt(m.getCreatedAt()).build();
    }
}
