package com.saas.platform.subscription.service;

import com.saas.platform.subscription.dto.ModuloDTO;
import com.saas.platform.subscription.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ModuloService {
    private final ModuloRepository repo;

    @Transactional(readOnly = true)
    public List<ModuloDTO> getActivos() {
        return repo.findByActivoTrueOrderByOrden().stream().map(m -> ModuloDTO.builder()
            .id(m.getId()).nombre(m.getNombre()).codigo(m.getCodigo())
            .descripcion(m.getDescripcion()).ruta(m.getRuta()).icono(m.getIcono())
            .orden(m.getOrden()).build()).collect(Collectors.toList());
    }
}
