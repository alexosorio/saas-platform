package com.saas.platform.notification.channel;

import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap; import java.util.Map;

/** Envío de WhatsApp vía Meta Cloud API. */
@Component
public class WhatsAppSender {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppSender.class);
    @Value("${whatsapp.phone-number-id:}") private String phoneNumberId;
    @Value("${whatsapp.access-token:}") private String accessToken;
    @Value("${whatsapp.api-version:v21.0}") private String apiVersion;
    @Value("${whatsapp.default-country-code:}") private String defaultCountryCode;
    private final RestTemplate rest = new RestTemplate();

    public void enviarTexto(String telefono, String texto) {
        if (phoneNumberId.isBlank() || accessToken.isBlank()) { log.warn("WhatsApp no configurado; se omite envío a {}", telefono); return; }
        Map<String,Object> body = new HashMap<>();
        body.put("messaging_product","whatsapp");
        String to = normalizar(telefono);
        body.put("to", to);
        body.put("type","text");
        Map<String,Object> t = new HashMap<>(); t.put("preview_url", true); t.put("body", texto);
        body.put("text", t);
        String url = String.format("https://graph.facebook.com/%s/%s/messages", apiVersion, phoneNumberId);
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setBearerAuth(accessToken);
        rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
        log.info("WhatsApp enviado a {}", telefono);
    }

    /** Normaliza a formato internacional sin '+'/ceros; antepone el indicativo por defecto si falta. */
    private String normalizar(String telefono) {
        if (telefono == null) return null;
        String n = telefono.trim().replaceAll("[^0-9]", "");   // solo dígitos
        n = n.replaceFirst("^00", "");                          // 00 internacional -> quitar
        if (defaultCountryCode != null && !defaultCountryCode.isBlank()) {
            String cc = defaultCountryCode.replaceAll("[^0-9]", "");
            if (!n.startsWith(cc)) {
                n = n.replaceFirst("^0+", "");                  // quitar ceros locales iniciales
                n = cc + n;                                     // anteponer indicativo país
            }
        }
        log.info("WhatsApp destino normalizado: '{}' -> '{}'", telefono, n);
        return n;
    }
}
