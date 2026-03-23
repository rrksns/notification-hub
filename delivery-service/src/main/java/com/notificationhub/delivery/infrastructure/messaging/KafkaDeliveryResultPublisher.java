package com.notificationhub.delivery.infrastructure.messaging;

import com.notificationhub.common.event.DeliveryResultEvent;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaDeliveryResultPublisher implements DeliveryResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDeliveryResultPublisher.class);
    private static final String TOPIC = "delivery-results";
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, DeliveryResultEvent> kafkaTemplate;

    public KafkaDeliveryResultPublisher(KafkaTemplate<String, DeliveryResultEvent> deliveryResultKafkaTemplate) {
        this.kafkaTemplate = deliveryResultKafkaTemplate;
    }

    @Override
    public void publishSuccess(DeliveryLog deliveryLog) {
        DeliveryResultEvent event = DeliveryResultEvent.success(
                deliveryLog.getId(),
                deliveryLog.getNotificationId(),
                deliveryLog.getTenantId(),
                deliveryLog.getChannel().name()
        );
        sendWithErrorHandling(event, deliveryLog.getNotificationId());
    }

    @Override
    public void publishFailure(DeliveryLog deliveryLog) {
        DeliveryResultEvent event = DeliveryResultEvent.failure(
                deliveryLog.getId(),
                deliveryLog.getNotificationId(),
                deliveryLog.getTenantId(),
                deliveryLog.getChannel().name(),
                deliveryLog.getFailureReason()
        );
        sendWithErrorHandling(event, deliveryLog.getNotificationId());
    }

    private void sendWithErrorHandling(DeliveryResultEvent event, String notificationId) {
        try {
            kafkaTemplate.send(TOPIC, event.tenantId(), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Published DeliveryResultEvent ({}): notificationId={}", event.status(), notificationId);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Failed to publish DeliveryResultEvent: notificationId={}, status={}", notificationId, event.status(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
