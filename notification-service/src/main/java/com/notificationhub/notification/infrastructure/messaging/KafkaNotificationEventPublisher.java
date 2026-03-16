package com.notificationhub.notification.infrastructure.messaging;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

    private static final String TOPIC = "notifications";

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public KafkaNotificationEventPublisher(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Notification notification) {
        NotificationEvent event = NotificationEvent.of(
                notification.getId(),
                notification.getTenantId(),
                notification.getChannel().name(),
                notification.getRecipient(),
                notification.getContent(),
                notification.getIdempotencyKey()
        );
        kafkaTemplate.send(TOPIC, notification.getTenantId(), event);
    }
}
