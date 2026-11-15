package com.saas.platform.notification.channel;

import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envío de SMS. Stub listo para conectar un proveedor (Twilio, Infobip, etc.).
 * Deja el punto de integración claro sin acoplar aún a un proveedor concreto.
 */
@Component
public class SmsSender {
    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);
    public void enviar(String telefono, String texto) {
        // TODO: integrar proveedor SMS (Twilio: POST /Messages con SID+Token).
        log.info("[SMS-STUB] Para {}: {}", telefono, texto);
    }
}
