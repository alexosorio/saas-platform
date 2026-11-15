package com.saas.platform.catalog.service;

import com.saas.platform.catalog.exception.BusinessException;
import com.saas.platform.email.EmailException;
import com.saas.platform.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fachada de correo del catálogo. Delega en el módulo compartido platform-email
 * (SendGrid + plantilla base). Convierte EmailException -> BusinessException para
 * mantener el manejo de errores existente.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailSender sender;

    /** Envía contenido envuelto en la plantilla base (encabezado + firma comunes). */
    public void enviar(String destinatario, String asunto, String contenidoHtml) {
        try { sender.enviar(destinatario, asunto, contenidoHtml); }
        catch (EmailException e) { throw new BusinessException(e.getMessage()); }
    }

    /** Envía un HTML completo sin envolver (compatibilidad). */
    public void enviarHtml(String destinatario, String asunto, String htmlBody) {
        try { sender.enviarHtmlCrudo(destinatario, asunto, htmlBody); }
        catch (EmailException e) { throw new BusinessException(e.getMessage()); }
    }
}
