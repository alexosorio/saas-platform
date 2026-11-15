package com.saas.platform.subscription.scheduler;

import com.saas.platform.subscription.service.AdminSuscripcionService;
import com.saas.platform.subscription.service.AvisosSuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada hora (y al arrancar el servicio):
 *  1) suspende las suscripciones vencidas sin pago, con aviso a sus administradores;
 *  2) envía los avisos de pronto vencimiento que correspondan hoy (7, 3, 1 y 0 días antes, configurable).
 * Ambos pasos son idempotentes: correr varias veces no repite suspensiones ni correos.
 * Además, la consulta de la suscripción actual suspende en el acto si ya venció.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VencimientosScheduler {

    private final AdminSuscripcionService service;
    private final AvisosSuscripcionService avisos;

    @Scheduled(cron = "${app.suscripciones.cron-vencimientos:0 5 * * * *}", zone = "${app.suscripciones.zona:America/Bogota}")
    public void ejecutar() {
        try {
            var r = service.procesarVencimientos();
            if (r.getSuspendidas() > 0) log.info("Vencimientos: {} suscripción(es) suspendida(s)", r.getSuspendidas());
        } catch (Exception e) { log.error("Error suspendiendo suscripciones vencidas: {}", e.getMessage(), e); }
        try {
            int n = avisos.enviarAvisosPrevios();
            if (n > 0) log.info("Avisos de pronto vencimiento enviados: {}", n);
        } catch (Exception e) { log.error("Error enviando avisos de vencimiento: {}", e.getMessage(), e); }
    }

    /** Al arrancar no se espera a la próxima hora. */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() { ejecutar(); }
}
