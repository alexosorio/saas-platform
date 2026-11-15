package com.saas.platform.catalog.service;

import com.saas.platform.catalog.dto.BodegaDTO;
import com.saas.platform.catalog.dto.CreateBodegaRequest;
import com.saas.platform.catalog.entity.Bodega;
import com.saas.platform.catalog.entity.Sede;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.BodegaRepository;
import com.saas.platform.catalog.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class BodegaService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    private final BodegaRepository repo;
    private final AuditoriaService auditoria;
    private final SedeRepository sedeRepo;

    @Transactional(readOnly = true)
    public List<BodegaDTO> getAll(Long orgId) {
        Map<Long, String> sedes = sedesDeOrg(orgId);
        if (sedes.isEmpty()) return List.of();
        return repo.findBySedeIdInAndActivoTrue(List.copyOf(sedes.keySet())).stream()
            .map(b -> toDTO(b, sedes)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BodegaDTO> getPorSede(Long sedeId, Long orgId) {
        Map<Long, String> sedes = sedesDeOrg(orgId);
        if (!sedes.containsKey(sedeId)) throw new ResourceNotFoundException("Sede no encontrada");
        return repo.findBySedeIdAndActivoTrue(sedeId).stream().map(b -> toDTO(b, sedes)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BodegaDTO getById(Long id, Long orgId) {
        Map<Long, String> sedes = sedesDeOrg(orgId);
        Bodega b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada"));
        if (!sedes.containsKey(b.getSedeId())) throw new ResourceNotFoundException("Bodega no encontrada");
        return toDTO(b, sedes);
    }

    @Transactional
    public BodegaDTO create(CreateBodegaRequest req, Long orgId) {
        Map<Long, String> sedes = sedesDeOrg(orgId);
        if (!sedes.containsKey(req.getSedeId()))
            throw new ResourceNotFoundException("La sede no pertenece a tu organizacion");
        Bodega b = Bodega.builder().sedeId(req.getSedeId()).nombre(req.getNombre())
            .codigo(req.getCodigo()).descripcion(req.getDescripcion())
            .capacidadM2(req.getCapacidadM2()).activo(true).build();
        log.info("Bodega creada: {} en sede {}", req.getNombre(), req.getSedeId());
        return toDTO(repo.save(b), sedes);
    }

    @Transactional
    public BodegaDTO update(Long id, CreateBodegaRequest req, Long orgId) {
        Map<Long, String> sedes = sedesDeOrg(orgId);
        Bodega b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada"));
        if (!sedes.containsKey(b.getSedeId())) throw new ResourceNotFoundException("Bodega no encontrada");
        if (req.getSedeId() != null) {
            if (!sedes.containsKey(req.getSedeId()))
                throw new ResourceNotFoundException("La sede no pertenece a tu organizacion");
            b.setSedeId(req.getSedeId());
        }
        b.setNombre(req.getNombre()); b.setCodigo(req.getCodigo());
        b.setDescripcion(req.getDescripcion()); b.setCapacidadM2(req.getCapacidadM2());
        return toDTO(repo.save(b), sedes);
    }

    @Transactional
    /** Las bodegas no se eliminan: se desactivan. No se puede desactivar una con existencias. */
    public void cambiarEstado(Long id, Long orgId, boolean activa) {
        Map<Long, String> sedes = sedesDeOrg(orgId);
        Bodega b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Bodega no encontrada"));
        if (!sedes.containsKey(b.getSedeId())) throw new ResourceNotFoundException("Bodega no encontrada");
        if (!activa) {
            Number stock = (Number) em.createNativeQuery(
                    "SELECT COALESCE(SUM(cantidad), 0) FROM inventario WHERE bodega_id = :b")
                .setParameter("b", id).getSingleResult();
            if (stock.longValue() > 0)
                throw new com.saas.platform.catalog.exception.InvalidOperationException(
                    "La bodega tiene " + stock.longValue() + " unidad(es) en existencia. Trasládalas antes de desactivarla.");
        }
        b.setActivo(activa); repo.save(b);
            auditoria.registrar(activa ? "BODEGA_ACTIVADA" : "BODEGA_DESACTIVADA", "bodega", id, null);
    }

    private Map<Long, String> sedesDeOrg(Long orgId) {
        Map<Long, String> m = new HashMap<>();
        for (Sede s : sedeRepo.findByCiudadOrganizacion_OrganizacionId(orgId)) m.put(s.getId(), s.getNombre());
        return m;
    }

    private BodegaDTO toDTO(Bodega b, Map<Long, String> sedes) {
        return BodegaDTO.builder().id(b.getId()).sedeId(b.getSedeId())
            .sedeNombre(sedes != null ? sedes.get(b.getSedeId()) : null)
            .nombre(b.getNombre()).codigo(b.getCodigo()).descripcion(b.getDescripcion())
            .capacidadM2(b.getCapacidadM2()).activo(b.getActivo()).createdAt(b.getCreatedAt()).build();
    }
}
