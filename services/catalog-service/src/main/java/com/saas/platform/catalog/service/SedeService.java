package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.CreateSedeRequest;
import com.saas.platform.catalog.dto.SedeDTO;
import com.saas.platform.catalog.entity.Ciudad;
import com.saas.platform.catalog.entity.CiudadOrganizacion;
import com.saas.platform.catalog.entity.Sede;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.CiudadOrganizacionRepository;
import com.saas.platform.catalog.repository.CiudadRepository;
import com.saas.platform.catalog.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class SedeService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private final SedeRepository repo;
    private final AuditoriaService auditoria;
    private final CiudadRepository ciudadRepo;
    private final CiudadOrganizacionRepository ciudadOrgRepo;

    @Transactional(readOnly = true)
    public List<SedeDTO> getAll(Long orgId) {
        return repo.findByCiudadOrganizacion_OrganizacionId(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SedeDTO> getActivas(Long orgId) {
        return repo.findByCiudadOrganizacion_OrganizacionIdAndActivoTrue(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SedeDTO getById(Long id, Long orgId) { return toDTO(find(id, orgId)); }

    @Transactional
    public SedeDTO create(CreateSedeRequest req, Long orgId) {
        CiudadOrganizacion co = resolverCiudadOrganizacion(orgId, req.getCiudadId());
        Sede s = Sede.builder().ciudadOrganizacion(co).nombre(req.getNombre())
            .direccion(req.getDireccion()).telefono(req.getTelefono()).email(req.getEmail())
            .activo(true).build();
        log.info("Sede creada: {} (org={} ciudad={})", req.getNombre(), orgId, req.getCiudadId());
        return toDTO(repo.save(s));
    }

    @Transactional
    public SedeDTO update(Long id, CreateSedeRequest req, Long orgId) {
        Sede s = find(id, orgId);
        s.setNombre(req.getNombre()); s.setDireccion(req.getDireccion());
        s.setTelefono(req.getTelefono()); s.setEmail(req.getEmail());
        if (req.getCiudadId() != null && !req.getCiudadId().equals(s.getCiudadOrganizacion().getCiudad().getId()))
            s.setCiudadOrganizacion(resolverCiudadOrganizacion(orgId, req.getCiudadId()));
        return toDTO(repo.save(s));
    }

    @Transactional
    /** Las sedes no se eliminan: se desactivan. No se puede desactivar una con bodegas activas. */
    public void cambiarEstado(Long id, Long orgId, boolean activa) {
        Sede s = find(id, orgId);
        if (!activa) {
            Number activas = (Number) em.createNativeQuery(
                    "SELECT COUNT(*) FROM bodegas WHERE sede_id = :s AND activo = TRUE")
                .setParameter("s", id).getSingleResult();
            if (activas.longValue() > 0)
                throw new com.saas.platform.catalog.exception.InvalidOperationException(
                    "La sede tiene " + activas.longValue() + " bodega(s) activa(s). Desactívalas antes de desactivar la sede.");
        }
        s.setActivo(activa); repo.save(s);
            auditoria.registrar(activa ? "SEDE_ACTIVADA" : "SEDE_DESACTIVADA", "sede", id, null);
    }

    private Sede find(Long id, Long orgId) {
        Sede s = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        if (!s.getCiudadOrganizacion().getOrganizacionId().equals(orgId))
            throw new ResourceNotFoundException("Sede no encontrada");
        return s;
    }

    private CiudadOrganizacion resolverCiudadOrganizacion(Long orgId, Long ciudadId) {
        return ciudadOrgRepo.findByOrganizacionIdAndCiudadId(orgId, ciudadId).orElseGet(() -> {
            Ciudad ciudad = ciudadRepo.findById(ciudadId)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudad no encontrada"));
            return ciudadOrgRepo.save(CiudadOrganizacion.builder().organizacionId(orgId).ciudad(ciudad).build());
        });
    }

    private SedeDTO toDTO(Sede s) {
        Ciudad ciudad = s.getCiudadOrganizacion() != null ? s.getCiudadOrganizacion().getCiudad() : null;
        return SedeDTO.builder().id(s.getId())
            .ciudadId(ciudad != null ? ciudad.getId() : null)
            .ciudadNombre(ciudad != null ? ciudad.getNombre() : null)
            .nombre(s.getNombre()).direccion(s.getDireccion())
            .telefono(s.getTelefono()).email(s.getEmail())
            .activo(s.getActivo()).createdAt(s.getCreatedAt()).build();
    }
}
