package com.saas.platform.subscription.service;

import com.saas.platform.subscription.dto.PagoDTO;
import com.saas.platform.subscription.dto.RegistrarPagoRequest;
import com.saas.platform.subscription.entity.Pago;
import com.saas.platform.subscription.entity.Suscripcion;
import com.saas.platform.subscription.exception.ResourceNotFoundException;
import com.saas.platform.subscription.repository.PagoRepository;
import com.saas.platform.subscription.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class PagoService {

    private final PagoRepository repo;
    private final AuditoriaService auditoria;
    private final SuscripcionRepository suscripcionRepo;

    @Transactional(readOnly = true)
    public List<PagoDTO> getPorSuscripcion(Long suscripcionId) {
        return repo.findBySuscripcionIdOrderByCreatedAtDesc(suscripcionId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PagoDTO> getPorOrganizacion(Long organizacionId) {
        // Encadenar via suscripciones de la org
        List<Suscripcion> ss = suscripcionRepo.findByOrganizacionIdOrderByCreatedAtDesc(organizacionId);
        return ss.stream()
            .flatMap(s -> repo.findBySuscripcionIdOrderByCreatedAtDesc(s.getId()).stream())
            .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PagoDTO registrar(RegistrarPagoRequest req, Long organizacionId) {
        Suscripcion s = suscripcionRepo.findFirstByOrganizacionIdOrderByCreatedAtDesc(organizacionId)
            .orElseThrow(() -> new ResourceNotFoundException("La organizacion no tiene suscripcion"));

        Pago p = Pago.builder()
            .suscripcionId(s.getId())
            .metodoPagoId(req.getMetodoPagoId())
            .monto(req.getMonto())
            .moneda(req.getMoneda() != null ? req.getMoneda() : "USD")
            .estado(Pago.Estado.pendiente)   // queda en validación: lo aprueba el administrador de la plataforma
            .tipo(req.getTipo() != null ? Pago.Tipo.valueOf(req.getTipo()) : Pago.Tipo.suscripcion)
            .descripcion(req.getDescripcion()).referenciaExterna(req.getReferenciaExterna())
            .metodo(req.getMetodo()).banco(req.getBanco()).comprobanteUrl(req.getComprobanteUrl())
            .ultimosDigitos(req.getUltimosDigitos()).fechaTransaccion(req.getFechaTransaccion())
            .referenciaInterna("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .fechaPago(LocalDate.now()).build();

        Pago saved = repo.save(p);

        log.info("Pago registrado (pendiente de validación) org={} suscripcion={} monto={}", organizacionId, s.getId(), req.getMonto());
        auditoria.registrar("PAGO_SUSCRIPCION_REGISTRADO", "pago", saved.getId(), java.util.Map.of("monto", String.valueOf(req.getMonto()), "metodo", String.valueOf(req.getMetodo()), "referencia", String.valueOf(req.getReferenciaExterna())), organizacionId);
        return toDTO(saved);
    }

    private PagoDTO toDTO(Pago p) {
        return PagoDTO.builder()
            .id(p.getId()).suscripcionId(p.getSuscripcionId()).metodoPagoId(p.getMetodoPagoId())
            .monto(p.getMonto()).moneda(p.getMoneda())
            .estado(p.getEstado() != null ? p.getEstado().name() : null)
            .tipo(p.getTipo() != null ? p.getTipo().name() : null)
            .descripcion(p.getDescripcion())
            .referenciaExterna(p.getReferenciaExterna()).referenciaInterna(p.getReferenciaInterna())
            .metodo(p.getMetodo()).banco(p.getBanco()).comprobanteUrl(p.getComprobanteUrl())
            .motivoRechazo(p.getMotivoRechazo()).fechaValidacion(p.getFechaValidacion())
            .ultimosDigitos(p.getUltimosDigitos()).fechaTransaccion(p.getFechaTransaccion())
            .fechaPago(p.getFechaPago()).fechaVencimiento(p.getFechaVencimiento())
            .createdAt(p.getCreatedAt()).build();
    }
}
