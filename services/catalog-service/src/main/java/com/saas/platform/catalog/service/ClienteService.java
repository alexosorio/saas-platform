package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.ClienteDTO;
import com.saas.platform.catalog.dto.CreateClienteRequest;
import com.saas.platform.catalog.entity.Cliente;
import com.saas.platform.catalog.exception.DuplicateResourceException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class ClienteService {

    private final ClienteRepository repo;

    @Transactional(readOnly = true)
    public List<ClienteDTO> getAll(Long orgId) {
        return repo.findByOrganizacionIdAndActivoTrue(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Clientes activos paginados por nombre; q busca por nombre, documento, correo o teléfono. */
    @Transactional(readOnly = true)
    public com.saas.platform.catalog.dto.PaginaDTO<ClienteDTO> paginado(Long orgId, String q, Integer page, Integer size) {
        final String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        org.springframework.data.jpa.domain.Specification<Cliente> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            ps.add(cb.equal(root.get("organizacionId"), orgId));
            ps.add(cb.isTrue(root.get("activo")));
            if (like != null) ps.add(cb.or(cb.like(cb.lower(root.get("nombre")), like), cb.like(cb.lower(root.get("documento")), like),
                cb.like(cb.lower(root.get("email")), like), cb.like(root.get("telefono"), like)));
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return com.saas.platform.catalog.dto.PaginaDTO.de(repo.findAll(spec, com.saas.platform.catalog.dto.PaginaDTO.solicitud(page, size, org.springframework.data.domain.Sort.by("nombre", "id"))), this::toDTO);
    }

    @Transactional(readOnly = true)
    public ClienteDTO getById(Long id, Long orgId) {
        Cliente c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (!c.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Cliente no encontrado");
        return toDTO(c);
    }

    @Transactional
    public ClienteDTO create(CreateClienteRequest req, Long orgId) {
        if (req.getDocumento() != null && !req.getDocumento().isBlank())
            repo.findByDocumentoAndOrganizacionId(req.getDocumento(), orgId).ifPresent(c -> {
                throw new DuplicateResourceException("Ya existe cliente con documento: " + req.getDocumento());
            });
        Cliente c = Cliente.builder()
            .organizacionId(orgId)
            .tipo(req.getTipo() != null ? Cliente.TipoCliente.valueOf(req.getTipo()) : Cliente.TipoCliente.minorista)
            .nombre(req.getNombre()).documento(req.getDocumento()).email(req.getEmail())
            .telefono(req.getTelefono()).direccion(req.getDireccion()).ciudadId(req.getCiudadId())
            .limiteCredito(req.getLimiteCredito() != null ? req.getLimiteCredito() : BigDecimal.ZERO)
            .activo(true).build();
        log.info("Cliente creado: {}", req.getNombre());
        return toDTO(repo.save(c));
    }

    @Transactional
    public ClienteDTO update(Long id, CreateClienteRequest req, Long orgId) {
        Cliente c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (!c.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Cliente no encontrado");
        c.setNombre(req.getNombre());
        if (req.getTipo() != null) c.setTipo(Cliente.TipoCliente.valueOf(req.getTipo()));
        c.setDocumento(req.getDocumento()); c.setEmail(req.getEmail());
        c.setTelefono(req.getTelefono()); c.setDireccion(req.getDireccion()); c.setCiudadId(req.getCiudadId());
        if (req.getLimiteCredito() != null) c.setLimiteCredito(req.getLimiteCredito());
        return toDTO(repo.save(c));
    }

    @Transactional
    public void delete(Long id, Long orgId) {
        Cliente c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (!c.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Cliente no encontrado");
        c.setActivo(false); repo.save(c);
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<ClienteDTO> buscar(Long orgId, String q) {
        return repo.buscar(orgId, q.trim(), org.springframework.data.domain.PageRequest.of(0, 20))
            .stream().map(this::toDTO).collect(java.util.stream.Collectors.toList());
    }

    private ClienteDTO toDTO(Cliente c) {
        return ClienteDTO.builder()
            .id(c.getId()).organizacionId(c.getOrganizacionId())
            .tipo(c.getTipo() != null ? c.getTipo().name() : null)
            .nombre(c.getNombre()).documento(c.getDocumento()).email(c.getEmail())
            .telefono(c.getTelefono()).direccion(c.getDireccion()).ciudadId(c.getCiudadId())
            .limiteCredito(c.getLimiteCredito()).activo(c.getActivo()).createdAt(c.getCreatedAt()).build();
    }
}
