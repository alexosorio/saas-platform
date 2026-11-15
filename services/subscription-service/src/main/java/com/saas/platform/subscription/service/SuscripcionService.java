package com.saas.platform.subscription.service;

import com.saas.platform.subscription.dto.*;
import com.saas.platform.subscription.entity.HistorialSuscripcion;
import com.saas.platform.subscription.entity.Plan;
import com.saas.platform.subscription.entity.Suscripcion;
import com.saas.platform.subscription.exception.InvalidOperationException;
import com.saas.platform.subscription.exception.ResourceNotFoundException;
import com.saas.platform.subscription.repository.HistorialSuscripcionRepository;
import com.saas.platform.subscription.repository.PlanRepository;
import com.saas.platform.subscription.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class SuscripcionService {

    private final SuscripcionRepository repo;
    private final PlanRepository planRepo;
    private final HistorialSuscripcionRepository historialRepo;
    private final ParametrosLector parametros;
    private final VencimientoService vencimientos;
    private final AvisosSuscripcionService avisos;

    /** Suscripcion activa/vigente de la organizacion (la mas reciente). */
    @Transactional   // puede suspender en el acto si ya venció
    public SuscripcionDTO getActual(Long organizacionId) {
        Suscripcion s = repo.findFirstByOrganizacionIdOrderByCreatedAtDesc(organizacionId)
            .orElseThrow(() -> new ResourceNotFoundException("La organizacion no tiene suscripcion"));
        // Si ya venció, se suspende en este momento (no espera al proceso programado ni al súper administrador)
        vencimientos.suspenderSiVencida(s);
        return toDTO(s);
    }

    @Transactional(readOnly = true)
    public List<SuscripcionDTO> getTodas(Long organizacionId) {
        return repo.findByOrganizacionIdOrderByCreatedAtDesc(organizacionId)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Crear o cambiar la suscripcion (upgrade/downgrade). Si ya existe una vigente,
     * la cancela y crea una nueva, dejando el cambio en el historial.
     */

    /**
     * Prueba gratuita para una organización NUEVA (asistente de registro). Solo una vez por organización.
     * Duración: parámetro suscripcion.dias_prueba (lo configura el dueño de la plataforma).
     */
    @Transactional
    public SuscripcionDTO iniciarPrueba(Long planId, String periodoTxt, Long organizacionId, Long usuarioId) {
        if (organizacionId == null) throw new InvalidOperationException("Tu usuario no pertenece a una organización");
        if (repo.findFirstByOrganizacionIdOrderByCreatedAtDesc(organizacionId).isPresent())
            throw new InvalidOperationException("La prueba gratuita solo está disponible para organizaciones nuevas");
        Plan plan = planRepo.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        if (!Boolean.TRUE.equals(plan.getActivo())) throw new InvalidOperationException("El plan no esta activo");

        int dias = parametros.entero("suscripcion.dias_prueba", null, 15);
        if (dias <= 0) throw new InvalidOperationException("La prueba gratuita no está habilitada");
        Suscripcion.Periodo periodo = "anual".equalsIgnoreCase(periodoTxt) ? Suscripcion.Periodo.anual : Suscripcion.Periodo.mensual;
        LocalDate hoy = LocalDate.now();
        Suscripcion s = repo.save(Suscripcion.builder()
            .organizacionId(organizacionId).plan(plan)
            .estado(Suscripcion.Estado.periodo_prueba).periodo(periodo)
            .fechaInicio(hoy).fechaFin(hoy.plusDays(dias)).autoRenovacion(true).build());
        historialRepo.save(HistorialSuscripcion.builder()
            .suscripcionId(s.getId()).planNuevoId(plan.getId())
            .accion(HistorialSuscripcion.Accion.creacion)
            .motivo("Prueba gratuita de " + dias + " días").usuarioId(usuarioId).build());
        log.info("Prueba gratuita de {} días para org {} (plan {})", dias, organizacionId, plan.getId());
        return toDTO(s);
    }

    @Transactional
    public SuscripcionDTO suscribir(CreateSuscripcionRequest req, Long organizacionId, Long usuarioId) {
        Plan planNuevo = planRepo.findById(req.getPlanId())
            .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        if (!Boolean.TRUE.equals(planNuevo.getActivo()))
            throw new InvalidOperationException("El plan no esta activo");

        Suscripcion vigente = repo.findFirstByOrganizacionIdAndEstadoInOrderByCreatedAtDesc(
            organizacionId,
            List.of(Suscripcion.Estado.activa, Suscripcion.Estado.periodo_prueba, Suscripcion.Estado.suspendida, Suscripcion.Estado.pendiente_pago)
        ).orElse(null);

        Long planAnteriorId = null;
        HistorialSuscripcion.Accion accion;

        if (vigente != null) {
            planAnteriorId = vigente.getPlan().getId();
            if (planAnteriorId.equals(planNuevo.getId()))
                throw new InvalidOperationException("La organizacion ya esta suscrita a este plan");
            accion = planNuevo.getPrecioMensual().compareTo(vigente.getPlan().getPrecioMensual()) > 0
                ? HistorialSuscripcion.Accion.upgrade
                : HistorialSuscripcion.Accion.downgrade;
            vigente.setEstado(Suscripcion.Estado.cancelada);
            vigente.setFechaFin(LocalDate.now());
            repo.save(vigente);
        } else {
            accion = HistorialSuscripcion.Accion.creacion;
        }

        Suscripcion.Periodo periodo = req.getPeriodo() != null
            ? Suscripcion.Periodo.valueOf(req.getPeriodo()) : Suscripcion.Periodo.mensual;
        LocalDate inicio = LocalDate.now();
        LocalDate fin = periodo == Suscripcion.Periodo.anual ? inicio.plusYears(1) : inicio.plusMonths(1);

        Suscripcion nueva = Suscripcion.builder()
            .organizacionId(organizacionId).plan(planNuevo)
            .estado(Suscripcion.Estado.pendiente_pago).periodo(periodo)
            .fechaInicio(inicio).fechaFin(fin)
            .autoRenovacion(req.getAutoRenovacion() != null ? req.getAutoRenovacion() : true)
            .build();
        Suscripcion saved = repo.save(nueva);

        historialRepo.save(HistorialSuscripcion.builder()
            .suscripcionId(saved.getId())
            .planAnteriorId(planAnteriorId).planNuevoId(planNuevo.getId())
            .accion(accion).motivo(req.getMotivo()).usuarioId(usuarioId).build());

        log.info("Suscripcion {} para org {} plan {}", accion, organizacionId, planNuevo.getId());
        return toDTO(saved);
    }

    @Transactional
    public SuscripcionDTO cancelar(Long organizacionId, String motivo, Long usuarioId) {
        Suscripcion s = repo.findFirstByOrganizacionIdAndEstadoInOrderByCreatedAtDesc(
            organizacionId,
            List.of(Suscripcion.Estado.activa, Suscripcion.Estado.periodo_prueba)
        ).orElseThrow(() -> new InvalidOperationException("No hay suscripcion activa"));

        s.setEstado(Suscripcion.Estado.cancelada);
        s.setAutoRenovacion(false);
        Suscripcion saved = repo.save(s);
        historialRepo.save(HistorialSuscripcion.builder()
            .suscripcionId(saved.getId())
            .planAnteriorId(saved.getPlan().getId()).planNuevoId(saved.getPlan().getId())
            .accion(HistorialSuscripcion.Accion.cancelacion).motivo(motivo).usuarioId(usuarioId).build());
        log.info("Suscripcion {} cancelada por org {}", saved.getId(), organizacionId);
        return toDTO(saved);
    }

    @Transactional
    public SuscripcionDTO reactivar(Long organizacionId, Long usuarioId) {
        Suscripcion s = repo.findFirstByOrganizacionIdOrderByCreatedAtDesc(organizacionId)
            .orElseThrow(() -> new ResourceNotFoundException("No hay suscripcion"));
        if (s.getEstado() != Suscripcion.Estado.cancelada && s.getEstado() != Suscripcion.Estado.suspendida)
            throw new InvalidOperationException("Solo se reactivan suscripciones canceladas o suspendidas");

        s.setEstado(Suscripcion.Estado.activa);
        s.setAutoRenovacion(true);
        if (s.getFechaFin() == null || s.getFechaFin().isBefore(LocalDate.now())) {
            LocalDate inicio = LocalDate.now();
            s.setFechaInicio(inicio);
            s.setFechaFin(s.getPeriodo() == Suscripcion.Periodo.anual ? inicio.plusYears(1) : inicio.plusMonths(1));
        }
        Suscripcion saved = repo.save(s);
        historialRepo.save(HistorialSuscripcion.builder()
            .suscripcionId(saved.getId()).planNuevoId(saved.getPlan().getId())
            .accion(HistorialSuscripcion.Accion.reactivacion).usuarioId(usuarioId).build());
        return toDTO(saved);
    }

    @Transactional
    public SuscripcionDTO setAutoRenovacion(Long organizacionId, boolean valor) {
        Suscripcion s = repo.findFirstByOrganizacionIdOrderByCreatedAtDesc(organizacionId)
            .orElseThrow(() -> new ResourceNotFoundException("No hay suscripcion"));
        s.setAutoRenovacion(valor);
        return toDTO(repo.save(s));
    }

    @Transactional(readOnly = true)
    public List<HistorialSuscripcionDTO> getHistorial(Long suscripcionId) {
        return historialRepo.findBySuscripcionIdOrderByCreatedAtDesc(suscripcionId)
            .stream().map(this::toHistorialDTO).collect(Collectors.toList());
    }

    private SuscripcionDTO toDTO(Suscripcion s) {
        return SuscripcionDTO.builder()
            .id(s.getId()).organizacionId(s.getOrganizacionId())
            .planId(s.getPlan() != null ? s.getPlan().getId() : null)
            .planNombre(s.getPlan() != null ? s.getPlan().getNombre() : null)
            .estado(s.getEstado() != null ? s.getEstado().name() : null)
            .periodo(s.getPeriodo() != null ? s.getPeriodo().name() : null)
            .fechaInicio(s.getFechaInicio()).fechaFin(s.getFechaFin())
            .autoRenovacion(s.getAutoRenovacion()).createdAt(s.getCreatedAt())
            .diasParaVencer(s.getFechaFin() != null ? java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), s.getFechaFin()) : null)
            .diasAviso(avisos.diasAviso().isEmpty() ? 0 : avisos.diasAviso().first())
            .build();
    }

    private HistorialSuscripcionDTO toHistorialDTO(HistorialSuscripcion h) {
        String planAntNombre = null, planNuevoNombre = null;
        if (h.getPlanAnteriorId() != null)
            planAntNombre = planRepo.findById(h.getPlanAnteriorId()).map(Plan::getNombre).orElse(null);
        if (h.getPlanNuevoId() != null)
            planNuevoNombre = planRepo.findById(h.getPlanNuevoId()).map(Plan::getNombre).orElse(null);

        return HistorialSuscripcionDTO.builder()
            .id(h.getId()).suscripcionId(h.getSuscripcionId())
            .planAnteriorId(h.getPlanAnteriorId()).planAnteriorNombre(planAntNombre)
            .planNuevoId(h.getPlanNuevoId()).planNuevoNombre(planNuevoNombre)
            .accion(h.getAccion() != null ? h.getAccion().name() : null)
            .motivo(h.getMotivo()).usuarioId(h.getUsuarioId())
            .montoCambio(h.getMontoCambio()).createdAt(h.getCreatedAt()).build();
    }
}
