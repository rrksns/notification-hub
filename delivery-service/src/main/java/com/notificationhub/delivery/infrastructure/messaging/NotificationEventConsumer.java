package com.notificationhub.delivery.infrastructure.messaging;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.delivery.domain.port.in.ProcessDeliveryUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final ProcessDeliveryUseCase processDeliveryUseCase;

    public NotificationEventConsumer(ProcessDeliveryUseCase processDeliveryUseCase) {
        this.processDeliveryUseCase = processDeliveryUseCase;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlq"
    )
    @KafkaListener(topics = "notifications", groupId = "delivery-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(NotificationEvent event) {
        log.info("Received NotificationEvent: notificationId={}, channel={}", event.notificationId(), event.channel());

        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                event.notificationId(),
                event.tenantId(),
                event.channel(),
                event.recipient(),
                event.content(),
                event.idempotencyKey()
        );

        ProcessDeliveryUseCase.Result result = processDeliveryUseCase.process(command);
        log.info("Delivery processed: deliveryLogId={}, status={}", result.deliveryLogId(), result.status());
    }
}
