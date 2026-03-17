package com.notificationhub.delivery.infrastructure.messaging;

import com.notificationhub.common.event.DeliveryResultEvent;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaDeliveryResultPublisher implements DeliveryResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDeliveryResultPublisher.class);
    private static final String TOPIC = "delivery-results";

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
        kafkaTemplate.send(TOPIC, deliveryLog.getTenantId(), event);
        log.info("Published DeliveryResultEvent (SUCCESS): notificationId={}", deliveryLog.getNotificationId());
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
        kafkaTemplate.send(TOPIC, deliveryLog.getTenantId(), event);
        log.info("Published DeliveryResultEvent (FAILED): notificationId={}, reason={}",
                deliveryLog.getNotificationId(), deliveryLog.getFailureReason());
    }
}
