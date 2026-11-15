package com.saas.platform.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Base64;

/**
 * Remitente único de correos de la plataforma, vía SendGrid.
 * Config por variables de entorno: SENDGRID_API_KEY, MAIL_FROM, MAIL_FROM_NAME.
 */
public class EmailSender implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${app.mail.from:noreply@tuapp.com}")
    private String from;

    @Value("${app.mail.from-name:SaaS Platform}")
    private String fromName;

    private final EmailLayout layout;

    public EmailSender(EmailLayout layout) { this.layout = layout; }

    /** Al arrancar: limpia la clave (espacios, saltos de línea, comillas) y deja en el log cuál se cargó, enmascarada. */
    @Override
    public void afterPropertiesSet() {
        if (apiKey != null) {
            apiKey = apiKey.trim();
            if (apiKey.length() >= 2 && (apiKey.startsWith("\"") && apiKey.endsWith("\"")
                                      || apiKey.startsWith("'") && apiKey.endsWith("'")))
                apiKey = apiKey.substring(1, apiKey.length() - 1).trim();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SendGrid: SENDGRID_API_KEY no está configurada en este servicio; no se podrán enviar correos.");
            return;
        }
        log.info("SendGrid: clave cargada {} ({} caracteres), remitente {} <{}>", enmascarar(apiKey), apiKey.length(), fromName, from);
        if (!apiKey.startsWith("SG."))
            log.warn("SendGrid: la clave no empieza por 'SG.'; probablemente no es una API key válida de SendGrid.");
    }

    private static String enmascarar(String k) {
        return k.length() <= 10 ? "***" : k.substring(0, 6) + "…" + k.substring(k.length() - 4);
    }

    /** Envía contenido ya envuelto en la plantilla base (recomendado). */
    public void enviar(String destinatario, String asunto, String contenidoHtml) {
        enviar(destinatario, asunto, asunto, contenidoHtml, null);
    }

    /** Igual, con la marca (nombre y logo) de la organización que envía. */
    public void enviar(String destinatario, String asunto, String contenidoHtml, Marca marca) {
        enviar(destinatario, asunto, asunto, contenidoHtml, marca);
    }

    /** Con un título visible distinto del asunto. */
    public void enviar(String destinatario, String asunto, String titulo, String contenidoHtml, Marca marca) {
        enviarHtmlCrudo(destinatario, asunto, layout.wrap(titulo, contenidoHtml, marca), marca);
    }

    /** Plantilla base de la plataforma (p. ej. para usar su botón dentro del contenido). */
    public EmailLayout layout() { return layout; }

    /** Envía un HTML completo sin envolver (para casos que ya traen su propio documento). */
    public void enviarHtmlCrudo(String destinatario, String asunto, String htmlCompleto) {
        enviarHtmlCrudo(destinatario, asunto, htmlCompleto, null);
    }

    /**
     * HTML completo; si la marca trae logo, se adjunta INLINE con Content-ID {@link Marca#CID_LOGO}
     * y el remitente se muestra con el nombre de la organización.
     */
    public void enviarHtmlCrudo(String destinatario, String asunto, String htmlCompleto, Marca marca) {
        if (apiKey == null || apiKey.isBlank())
            throw new EmailException("El envío de correo no está configurado (falta SENDGRID_API_KEY)");
        if (destinatario == null || destinatario.isBlank())
            throw new EmailException("Destinatario de correo vacío");

        String nombreRemitente = marca != null && marca.nombre() != null && !marca.nombre().isBlank()
            ? marca.nombre() : fromName;
        Mail mail = new Mail(new Email(from, nombreRemitente), asunto, new Email(destinatario),
                             new Content("text/html", htmlCompleto));
        if (marca != null && marca.tieneLogo() && htmlCompleto.contains("cid:" + Marca.CID_LOGO)) {
            Attachments logo = new Attachments();
            logo.setContent(Base64.getEncoder().encodeToString(marca.logo()));
            logo.setType(marca.logoTipo());
            logo.setFilename(marca.logoTipo().endsWith("jpeg") ? "logo.jpg" : "logo.png");
            logo.setDisposition("inline");
            logo.setContentId(Marca.CID_LOGO);
            mail.addAttachments(logo);
        }
        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            int code = response.getStatusCode();
            if (code >= 400) {
                log.error("SendGrid error {}: {}", code, response.getBody());
                String motivo = switch (code) {
                    case 401 -> "SendGrid rechazó la API key (401). Verifique SENDGRID_API_KEY del servicio que envía "
                              + "(clave cargada: " + enmascarar(apiKey) + "): puede estar mal copiada, revocada o ser de otra cuenta.";
                    case 403 -> "SendGrid denegó el envío (403): la API key no tiene permiso 'Mail Send' o el remitente '"
                              + from + "' no está verificado como Sender Identity.";
                    case 429 -> "SendGrid limitó el envío por exceso de solicitudes (429).";
                    default  -> "SendGrid respondió con código " + code + ": " + response.getBody();
                };
                throw new EmailException(motivo);
            }
            log.info("Correo enviado a {} (status {})", destinatario, code);
        } catch (IOException ex) {
            log.error("Error enviando correo: {}", ex.getMessage());
            throw new EmailException("No se pudo enviar el correo: " + ex.getMessage());
        }
    }
}
