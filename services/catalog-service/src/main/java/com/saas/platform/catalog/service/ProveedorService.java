package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CreateProveedorRequest;
import com.saas.platform.catalog.dto.ProveedorDTO;
import com.saas.platform.catalog.entity.Proveedor;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class ProveedorService {

    private final ProveedorRepository repo;

    @Transactional(readOnly = true)
    public List<ProveedorDTO> getAll(Long orgId) {
        return repo.findByOrganizacionId(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProveedorDTO> getActivos(Long orgId) {
        return repo.findByOrganizacionIdAndActivoTrue(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProveedorDTO getById(Long id, Long orgId) {
        Proveedor p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!p.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Proveedor no encontrado");
        return toDTO(p);
    }

    @Transactional
    public ProveedorDTO create(CreateProveedorRequest req, Long orgId) {
        Proveedor p = Proveedor.builder()
            .organizacionId(orgId).nombre(req.getNombre())
            .identificacion(req.getIdentificacion()).telefono(req.getTelefono())
            .email(req.getEmail()).direccion(req.getDireccion())
            .contacto(req.getContacto()).notas(req.getNotas()).activo(true).build();
        log.info("Proveedor creado: {}", req.getNombre());
        return toDTO(repo.save(p));
    }

    @Transactional
    public ProveedorDTO update(Long id, CreateProveedorRequest req, Long orgId) {
        Proveedor p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!p.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Proveedor no encontrado");
        p.setNombre(req.getNombre()); p.setIdentificacion(req.getIdentificacion());
        p.setTelefono(req.getTelefono()); p.setEmail(req.getEmail());
        p.setDireccion(req.getDireccion()); p.setContacto(req.getContacto()); p.setNotas(req.getNotas());
        return toDTO(repo.save(p));
    }

    @Transactional
    public void delete(Long id, Long orgId) {
        Proveedor p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        if (!p.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Proveedor no encontrado");
        p.setActivo(false); repo.save(p);   // soft delete
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<ProveedorDTO> buscar(Long orgId, String q) {
        return repo.buscar(orgId, q.trim(), org.springframework.data.domain.PageRequest.of(0, 20))
            .stream().map(this::toDTO).collect(java.util.stream.Collectors.toList());
    }

    private ProveedorDTO toDTO(Proveedor p) {
        return ProveedorDTO.builder()
            .id(p.getId()).organizacionId(p.getOrganizacionId()).nombre(p.getNombre())
            .identificacion(p.getIdentificacion()).telefono(p.getTelefono()).email(p.getEmail())
            .direccion(p.getDireccion()).contacto(p.getContacto()).notas(p.getNotas())
            .activo(p.getActivo()).createdAt(p.getCreatedAt()).build();
    }
}
