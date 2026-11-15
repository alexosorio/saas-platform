package com.saas.platform.subscription.service;

import com.saas.platform.subscription.dto.CreateMetodoPagoRequest;
import com.saas.platform.subscription.dto.MetodoPagoDTO;
import com.saas.platform.subscription.entity.MetodoPago;
import com.saas.platform.subscription.exception.ResourceNotFoundException;
import com.saas.platform.subscription.exception.InvalidOperationException;
import com.saas.platform.subscription.repository.MetodoPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class MetodoPagoService {

    private final MetodoPagoRepository repo;

    @Transactional(readOnly = true)
    public List<MetodoPagoDTO> getAll(Long orgId) {
        return repo.findByOrganizacionIdAndActivoTrue(orgId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public MetodoPagoDTO create(CreateMetodoPagoRequest req, Long orgId) {
        if (!Boolean.TRUE.equals(req.getAutorizacionDatos()))
            throw new InvalidOperationException("Se requiere la autorización de tratamiento de datos (habeas data)");

        MetodoPago m = MetodoPago.builder()
            .organizacionId(orgId)
            .tipo(MetodoPago.Tipo.valueOf(req.getTipo()))
            .proveedor(req.getProveedor()).ultimosDigitos(req.getUltimosDigitos())
            .token(req.getToken()).nombreTitular(req.getNombreTitular())
            .fechaExpiracion(req.getFechaExpiracion())
            .predeterminado(req.getPredeterminado() != null ? req.getPredeterminado() : false)
            .autorizacionDatos(Boolean.TRUE.equals(req.getAutorizacionDatos()))
            .fechaAutorizacion(req.getFechaAutorizacion() != null ? req.getFechaAutorizacion() : java.time.LocalDateTime.now())
            .versionPolitica(req.getVersionPolitica() != null ? req.getVersionPolitica() : "1.0")
            .activo(true).build();

        if (Boolean.TRUE.equals(m.getPredeterminado())) {
            repo.findByOrganizacionIdAndPredeterminadoTrue(orgId).ifPresent(prev -> {
                prev.setPredeterminado(false); repo.save(prev);
            });
        }
        return toDTO(repo.save(m));
    }

    @Transactional
    public MetodoPagoDTO marcarPredeterminado(Long id, Long orgId) {
        MetodoPago m = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Metodo de pago no encontrado"));
        if (!m.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Metodo no encontrado");
        repo.findByOrganizacionIdAndPredeterminadoTrue(orgId).ifPresent(prev -> {
            prev.setPredeterminado(false); repo.save(prev);
        });
        m.setPredeterminado(true);
        return toDTO(repo.save(m));
    }

    @Transactional
    public void delete(Long id, Long orgId) {
        MetodoPago m = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Metodo no encontrado"));
        if (!m.getOrganizacionId().equals(orgId)) throw new ResourceNotFoundException("Metodo no encontrado");
        m.setActivo(false); repo.save(m);
    }

    private MetodoPagoDTO toDTO(MetodoPago m) {
        return MetodoPagoDTO.builder()
            .id(m.getId()).organizacionId(m.getOrganizacionId())
            .tipo(m.getTipo() != null ? m.getTipo().name() : null)
            .proveedor(m.getProveedor()).ultimosDigitos(m.getUltimosDigitos())
            .nombreTitular(m.getNombreTitular()).fechaExpiracion(m.getFechaExpiracion())
            .predeterminado(m.getPredeterminado()).activo(m.getActivo())
            .createdAt(m.getCreatedAt()).build();
    }
}
