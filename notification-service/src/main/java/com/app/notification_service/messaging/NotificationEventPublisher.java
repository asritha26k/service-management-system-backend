package com.app.notification_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.app.notification_service.config.RabbitConfig;
import com.app.notification_service.messaging.event.NotificationEvent;

@Component
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(NotificationEvent event) {
        log.info("Publishing notification event for user {} with type {}", event.getUserId(), event.getNotificationType());
        rabbitTemplate.convertAndSend(
            RabbitConfig.NOTIFICATION_EXCHANGE,
            RabbitConfig.NOTIFICATION_EMAIL_ROUTING_KEY,
            event
        );
    }
}
