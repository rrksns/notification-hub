package com.notificationhub.notification.infrastructure.messaging;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaNotificationEventPublisher implements NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaNotificationEventPublisher.class);
    private static final String TOPIC = "notifications";
    private static final long SEND_TIMEOUT_SECONDS = 5;

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
        try {
            kafkaTemplate.send(TOPIC, notification.getTenantId(), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Published NotificationEvent: notificationId={}, tenantId={}", notification.getId(), notification.getTenantId());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Failed to publish NotificationEvent: notificationId={}, tenantId={}", notification.getId(), notification.getTenantId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Kafka publish failed for notification: " + notification.getId(), e);
        }
    }
}
