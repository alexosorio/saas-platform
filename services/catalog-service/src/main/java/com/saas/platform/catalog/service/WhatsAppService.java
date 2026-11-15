package com.saas.platform.catalog.service;

import com.saas.platform.catalog.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Envío de mensajes de WhatsApp usando la WhatsApp Cloud API de Meta (oficial),
 * vía HTTP directo (no requiere SDK adicional).
 *
 * Config por variables de entorno:
 *   WHATSAPP_PHONE_NUMBER_ID  (ID del número emisor en Meta)
 *   WHATSAPP_ACCESS_TOKEN     (token permanente de la app de Meta)
 *   WHATSAPP_API_VERSION      (por defecto v21.0)
 *
 * IMPORTANTE sobre plantillas:
 *  - Meta sólo permite mensajes de TEXTO libre dentro de la ventana de 24h
 *    posterior a que el cliente te escriba.
 *  - Para iniciar la conversación (caso "te comparto el catálogo"), Meta exige
 *    una PLANTILLA pre-aprobada. Por eso este servicio soporta ambos:
 *    enviarTexto(...) y enviarPlantilla(...).
 */
@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.api-version:v21.0}")
    private String apiVersion;

    private final RestTemplate rest = new RestTemplate();

    /** Mensaje de texto libre (válido dentro de la ventana de 24h). */
    public void enviarTexto(String telefono, String texto) {
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalizar(telefono));
        body.put("type", "text");
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("preview_url", true);
        textObj.put("body", texto);
        body.put("text", textObj);
        enviar(body, telefono);
    }

    /**
     * Mensaje de plantilla (para iniciar conversación). La plantilla debe existir
     * y estar aprobada en Meta. Se pasa el nombre, el idioma y los parámetros del body.
     */
    public void enviarPlantilla(String telefono, String plantilla, String idioma, java.util.List<String> parametros) {
        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalizar(telefono));
        body.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", plantilla);
        Map<String, Object> lang = new HashMap<>();
        lang.put("code", idioma != null ? idioma : "es");
        template.put("language", lang);

        if (parametros != null && !parametros.isEmpty()) {
            java.util.List<Map<String, Object>> params = new java.util.ArrayList<>();
            for (String p : parametros) {
                Map<String, Object> param = new HashMap<>();
                param.put("type", "text");
                param.put("text", p);
                params.add(param);
            }
            Map<String, Object> component = new HashMap<>();
            component.put("type", "body");
            component.put("parameters", params);
            template.put("components", java.util.List.of(component));
        }
        body.put("template", template);
        enviar(body, telefono);
    }

    private void enviar(Map<String, Object> body, String telefono) {
        if (phoneNumberId == null || phoneNumberId.isBlank() || accessToken == null || accessToken.isBlank())
            throw new BusinessException("WhatsApp no está configurado (faltan WHATSAPP_PHONE_NUMBER_ID / WHATSAPP_ACCESS_TOKEN)");
        if (telefono == null || telefono.isBlank())
            throw new BusinessException("Teléfono de WhatsApp vacío");

        String url = String.format("https://graph.facebook.com/%s/%s/messages", apiVersion, phoneNumberId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            log.info("WhatsApp enviado a {} (status {})", telefono, resp.getStatusCodeValue());
        } catch (HttpStatusCodeException ex) {
            log.error("WhatsApp API error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException("WhatsApp API respondió: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Error enviando WhatsApp: {}", ex.getMessage());
            throw new BusinessException("No se pudo enviar el WhatsApp: " + ex.getMessage());
        }
    }

    /** Deja sólo dígitos (Meta espera el número en formato internacional sin '+'). */
    private String normalizar(String telefono) {
        return telefono == null ? null : telefono.replaceAll("[^0-9]", "");
    }
}
