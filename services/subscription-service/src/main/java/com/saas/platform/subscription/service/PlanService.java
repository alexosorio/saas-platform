package com.saas.platform.subscription.service;

import com.saas.platform.subscription.dto.ModuloDTO;
import com.saas.platform.subscription.dto.PlanDTO;
import com.saas.platform.subscription.entity.Plan;
import com.saas.platform.subscription.exception.ResourceNotFoundException;
import com.saas.platform.subscription.repository.PlanModuloRepository;
import com.saas.platform.subscription.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PlanService {

    private final PlanRepository repo;
    private final PlanModuloRepository planModuloRepo;

    @Transactional(readOnly = true)
    public List<PlanDTO> getActivos() {
        return repo.findByActivoTrueOrderByOrden().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanDTO getById(Long id) {
        Plan p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        PlanDTO dto = toDTO(p);
        // Incluir modulos del plan
        List<ModuloDTO> modulos = planModuloRepo.findByPlanId(p.getId()).stream()
            .map(pm -> ModuloDTO.builder()
                .id(pm.getModulo().getId()).nombre(pm.getModulo().getNombre())
                .codigo(pm.getModulo().getCodigo()).descripcion(pm.getModulo().getDescripcion())
                .ruta(pm.getModulo().getRuta()).icono(pm.getModulo().getIcono())
                .orden(pm.getModulo().getOrden()).build())
            .collect(Collectors.toList());
        dto.setModulos(modulos);
        return dto;
    }

    private PlanDTO toDTO(Plan p) {
        return PlanDTO.builder()
            .id(p.getId()).nombre(p.getNombre()).descripcion(p.getDescripcion())
            .precioMensual(p.getPrecioMensual()).precioAnual(p.getPrecioAnual())
            .maxUsuarios(p.getMaxUsuarios()).almacenamientoGb(p.getAlmacenamientoGb())
            .activo(p.getActivo()).orden(p.getOrden()).build();
    }
}
