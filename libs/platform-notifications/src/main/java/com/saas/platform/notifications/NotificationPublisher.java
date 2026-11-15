package com.saas.platform.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/** Publica notificaciones en la cola. Lo usan los servicios productores. */
public class NotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publicar(NotificationMessage msg) {
        rabbitTemplate.convertAndSend(NotificationsConfig.EXCHANGE, NotificationsConfig.ROUTING_KEY, msg);
        log.info("Notificación encolada canal={} destino={}", msg.getCanal(), msg.getDestinatario());
    }
}
