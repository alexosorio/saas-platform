package com.saas.platform.email;

import org.springframework.beans.factory.annotation.Value;

/**
 * Plantilla HTML base común a toda la plataforma: encabezado con la marca de la
 * organización (logo o nombre), título, cuerpo y pie. Compatible con clientes de correo
 * (tablas y estilos en línea). Configurable por properties:
 *   app.mail.brand-name, app.mail.brand-color, app.mail.accent-color, app.mail.footer
 */
public class EmailLayout {

    @Value("${app.mail.brand-name:SaaS Platform}")
    private String brandName;

    @Value("${app.mail.brand-color:#693a84}")
    private String brandColor;

    @Value("${app.mail.accent-color:#a871bd}")
    private String accentColor;

    @Value("${app.mail.footer:Este es un mensaje automático. Por favor no respondas a este correo.}")
    private String footer;

    /** Compatibilidad: sin marca de organización (usa el nombre de la plataforma). */
    public String wrap(String titulo, String contenidoHtml) {
        return wrap(titulo, contenidoHtml, null);
    }

    /** Envuelve el contenido en el marco con la marca de la organización. */
    public String wrap(String titulo, String contenidoHtml, Marca marca) {
        String nombre = marca != null && marca.nombre() != null && !marca.nombre().isBlank() ? marca.nombre() : brandName;
        String encabezado = marca != null && marca.tieneLogo()
            ? "<img src=\"cid:" + Marca.CID_LOGO + "\" alt=\"" + esc(nombre) + "\" height=\"56\" "
              + "style=\"display:block;height:56px;max-height:56px;width:auto;max-width:240px;border:0;outline:none;\">"
            : "<div style=\"font-size:20px;font-weight:bold;color:" + brandColor + ";\">" + esc(nombre) + "</div>";
        String safeTitulo = titulo == null ? "" : esc(titulo);
        String firma = marca != null && marca.nombre() != null && !marca.nombre().isBlank() && !marca.nombre().equals(brandName)
            ? "Enviado por " + esc(nombre) + " a través de " + esc(brandName) + ". " : "";

        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>" + safeTitulo + "</title></head>"
            + "<body style=\"margin:0;padding:0;background:#f4f1f6;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f1f6;padding:24px 12px;\"><tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"max-width:600px;width:100%;background:#ffffff;border-radius:12px;overflow:hidden;font-family:Arial,Helvetica,sans-serif;\">"
            // Encabezado: logo de la organización (o su nombre) sobre fondo blanco + franja de color
            + "<tr><td style=\"padding:22px 32px 18px;border-bottom:4px solid " + accentColor + ";\">" + encabezado + "</td></tr>"
            + (safeTitulo.isEmpty() ? "" :
               "<tr><td style=\"padding:26px 32px 0;color:" + brandColor + ";font-size:21px;font-weight:bold;\">" + safeTitulo + "</td></tr>")
            + "<tr><td style=\"padding:14px 32px 28px;color:#333333;font-size:15px;line-height:1.6;\">"
            + (contenidoHtml == null ? "" : contenidoHtml) + "</td></tr>"
            + "<tr><td style=\"padding:16px 32px;background:#faf7fb;color:#888888;font-size:12px;line-height:1.5;text-align:center;\">"
            + firma + esc(footer) + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    /** Botón compatible con Outlook para usar dentro del contenido. */
    public String boton(String texto, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\" style=\"margin:8px auto 20px;\"><tr>"
            + "<td style=\"border-radius:8px;background:" + accentColor + ";\">"
            + "<a href=\"" + esc(url) + "\" target=\"_blank\" style=\"display:inline-block;padding:13px 30px;font-size:15px;"
            + "font-weight:bold;color:#ffffff;text-decoration:none;border-radius:8px;\">" + esc(texto) + "</a></td></tr></table>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
