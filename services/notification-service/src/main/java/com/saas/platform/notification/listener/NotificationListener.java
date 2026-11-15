package com.saas.platform.notification.listener;

import com.saas.platform.email.EmailSender;
import com.saas.platform.notification.channel.SmsSender;
import com.saas.platform.notification.marca.MarcaCliente;
import com.saas.platform.notification.channel.WhatsAppSender;
import com.saas.platform.notifications.NotificationMessage;
import com.saas.platform.notifications.NotificationsConfig;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consume la cola y despacha por canal (correo/WhatsApp/SMS). */
@Component
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);
    private final EmailSender emailSender;
    private final WhatsAppSender whatsAppSender;
    private final SmsSender smsSender;
    private final MarcaCliente marcaCliente;

    public NotificationListener(EmailSender emailSender, WhatsAppSender whatsAppSender, SmsSender smsSender,
                                MarcaCliente marcaCliente) {
        this.marcaCliente = marcaCliente;
        this.emailSender = emailSender;
        this.whatsAppSender = whatsAppSender;
        this.smsSender = smsSender;
    }

    @RabbitListener(queues = NotificationsConfig.QUEUE)
    public void recibir(NotificationMessage msg) {
        if (msg == null || msg.getCanal() == null) { log.warn("Mensaje inválido en la cola"); return; }
        try {
            switch (msg.getCanal()) {
                case EMAIL -> emailSender.enviar(msg.getDestinatario(), msg.getAsunto(), msg.getContenido(),
                                  marcaCliente.marca(msg.getOrganizacionId(), msg.getOrganizacionNombre()));
                case WHATSAPP -> whatsAppSender.enviarTexto(msg.getDestinatario(), msg.getContenido());
                case SMS -> smsSender.enviar(msg.getDestinatario(), msg.getContenido());
            }
        } catch (Exception e) {
            // Al relanzar, RabbitMQ reintenta / envía a DLQ según configuración.
            log.error("Fallo al enviar notificación (canal={}, destino={}): {}", msg.getCanal(), msg.getDestinatario(), e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
