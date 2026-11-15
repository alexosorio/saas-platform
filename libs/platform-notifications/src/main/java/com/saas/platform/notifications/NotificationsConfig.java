package com.saas.platform.notifications;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Declara exchange/cola/binding + conversor JSON y el publicador. Autoconfigurado. */
@AutoConfiguration
public class NotificationsConfig {

    public static final String EXCHANGE = "notifications.exchange";
    public static final String QUEUE = "notifications.queue";
    public static final String ROUTING_KEY = "notifications.send";

    @Bean public DirectExchange notificationsExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean public Queue notificationsQueue() { return QueueBuilder.durable(QUEUE).build(); }
    @Bean public Binding notificationsBinding(Queue notificationsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(notificationsExchange).with(ROUTING_KEY);
    }

    @Bean public MessageConverter jsonMessageConverter() { return new JacksonJsonMessageConverter(); }

    @Bean public RabbitTemplate notificationsRabbitTemplate(ConnectionFactory cf, MessageConverter jsonMessageConverter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonMessageConverter);
        return t;
    }

    @Bean public NotificationPublisher notificationPublisher(RabbitTemplate notificationsRabbitTemplate) {
        return new NotificationPublisher(notificationsRabbitTemplate);
    }
}
