package com.saas.platform.notifications;

import java.io.Serializable;
import java.util.Map;

/**
 * Mensaje de notificación que viaja por la cola. Es agnóstico del canal:
 * el notification-service lo empaqueta y envía como correo, SMS o WhatsApp.
 */
public class NotificationMessage implements Serializable {
    private Channel canal;          // EMAIL | SMS | WHATSAPP
    private String destinatario;    // correo o teléfono según el canal
    private String asunto;          // asunto (correo)
    private String contenido;       // cuerpo: HTML (correo) o texto (SMS/WhatsApp)
    private String plantilla;       // opcional: nombre de plantilla WhatsApp/otra
    private Map<String, String> datos; // opcional: variables para plantillas
    private String origen;          // servicio que la generó (traza)
    private Long organizacionId;    // opcional: organización que envía (su logo va en el correo)
    private String organizacionNombre; // opcional: nombre que firma el correo

    public NotificationMessage() {}

    public static NotificationMessage email(String to, String asunto, String htmlContenido) {
        NotificationMessage m = new NotificationMessage();
        m.canal = Channel.EMAIL; m.destinatario = to; m.asunto = asunto; m.contenido = htmlContenido;
        return m;
    }
    /** Correo con la marca (logo y nombre) de la organización que lo envía. */
    public static NotificationMessage email(String to, String asunto, String htmlContenido,
                                            Long organizacionId, String organizacionNombre) {
        NotificationMessage m = email(to, asunto, htmlContenido);
        m.organizacionId = organizacionId; m.organizacionNombre = organizacionNombre;
        return m;
    }
    public static NotificationMessage whatsapp(String telefono, String texto) {
        NotificationMessage m = new NotificationMessage();
        m.canal = Channel.WHATSAPP; m.destinatario = telefono; m.contenido = texto;
        return m;
    }
    public static NotificationMessage sms(String telefono, String texto) {
        NotificationMessage m = new NotificationMessage();
        m.canal = Channel.SMS; m.destinatario = telefono; m.contenido = texto;
        return m;
    }

    public Channel getCanal() { return canal; }
    public void setCanal(Channel canal) { this.canal = canal; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getPlantilla() { return plantilla; }
    public void setPlantilla(String plantilla) { this.plantilla = plantilla; }
    public Map<String, String> getDatos() { return datos; }
    public void setDatos(Map<String, String> datos) { this.datos = datos; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public Long getOrganizacionId() { return organizacionId; }
    public void setOrganizacionId(Long organizacionId) { this.organizacionId = organizacionId; }
    public String getOrganizacionNombre() { return organizacionNombre; }
    public void setOrganizacionNombre(String organizacionNombre) { this.organizacionNombre = organizacionNombre; }
}
