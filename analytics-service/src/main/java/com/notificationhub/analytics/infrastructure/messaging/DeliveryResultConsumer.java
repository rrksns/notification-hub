package com.notificationhub.analytics.infrastructure.messaging;

import com.notificationhub.common.event.DeliveryResultEvent;
import com.notificationhub.analytics.domain.port.in.RecordDeliveryEventUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DeliveryResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeliveryResultConsumer.class);

    private final RecordDeliveryEventUseCase recordDeliveryEventUseCase;

    public DeliveryResultConsumer(RecordDeliveryEventUseCase recordDeliveryEventUseCase) {
        this.recordDeliveryEventUseCase = recordDeliveryEventUseCase;
    }

    @KafkaListener(topics = "delivery-results", groupId = "analytics-service")
    public void consume(DeliveryResultEvent event) {
        log.info("Received delivery result: {} status={}", event.deliveryLogId(), event.status());
        RecordDeliveryEventUseCase.Command cmd = new RecordDeliveryEventUseCase.Command(
                event.deliveryLogId(),
                event.notificationId(),
                event.tenantId(),
                event.channel(),
                event.status(),
                event.failureReason(),
                event.occurredAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        );
        recordDeliveryEventUseCase.record(cmd);
    }
}
