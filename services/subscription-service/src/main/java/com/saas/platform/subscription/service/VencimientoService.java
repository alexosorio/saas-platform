package com.saas.platform.subscription.service;

import com.saas.platform.subscription.entity.HistorialSuscripcion;
import com.saas.platform.subscription.entity.Suscripcion;
import com.saas.platform.subscription.repository.HistorialSuscripcionRepository;
import com.saas.platform.subscription.repository.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Regla única de vencimiento: una suscripción vigente (activa o en prueba) cuya fecha de fin ya pasó
 * (más los días de gracia, 0 por defecto) se SUSPENDE de inmediato. La usan el proceso programado y
 * la consulta de la suscripción actual, así el bloqueo no depende del súper administrador.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VencimientoService {

    private final SuscripcionRepository suscripcionRepo;
    private final HistorialSuscripcionRepository historialRepo;
    private final ParametrosLector parametros;
    private final AuditoriaService auditoria;
    private final AvisosSuscripcionService avisos;

    @Value("${app.suscripciones.dias-gracia:0}")
    private int diasGraciaPorDefecto;

    public int diasGracia() {
        return Math.max(0, parametros.entero("suscripcion.dias_gracia", null, diasGraciaPorDefecto));
    }

    public static boolean vigente(Suscripcion s) {
        return s.getEstado() == Suscripcion.Estado.activa || s.getEstado() == Suscripcion.Estado.periodo_prueba;
    }

    /** ¿Ya pasó la fecha de fin (más la gracia)? El último día pagado es la fecha de fin. */
    public boolean vencida(Suscripcion s, int gracia) {
        return s.getFechaFin() != null && s.getFechaFin().plusDays(gracia).isBefore(LocalDate.now());
    }

    /** Suspende si corresponde; devuelve true si la suspendió. Avisa por correo a los administradores. */
    @Transactional
    public boolean suspenderSiVencida(Suscripcion s) {
        int gracia = diasGracia();
        if (!vigente(s) || !vencida(s, gracia)) return false;
        s.setEstado(Suscripcion.Estado.suspendida);
        suscripcionRepo.save(s);
        String motivo = "Suspensión automática por falta de pago (venció el " + s.getFechaFin()
            + (gracia > 0 ? ", " + gracia + " días de gracia)" : ")");
        historialRepo.save(HistorialSuscripcion.builder()
            .suscripcionId(s.getId())
            .planAnteriorId(s.getPlan() != null ? s.getPlan().getId() : null)
            .planNuevoId(s.getPlan() != null ? s.getPlan().getId() : null)
            .accion(HistorialSuscripcion.Accion.suspension).motivo(motivo).build());
        auditoria.registrar("SUSCRIPCION_SUSPENDIDA_AUTOMATICA", "suscripcion", s.getId(), Map.of("motivo", motivo), s.getOrganizacionId());
        avisos.avisarSuspension(s);
        log.warn("Suscripción {} (organización {}) suspendida: {}", s.getId(), s.getOrganizacionId(), motivo);
        return true;
    }
}
