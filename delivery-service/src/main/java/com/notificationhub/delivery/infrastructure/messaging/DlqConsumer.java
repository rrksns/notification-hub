package com.notificationhub.delivery.infrastructure.messaging;

import com.notificationhub.common.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    @KafkaListener(topics = "notifications.dlq", groupId = "delivery-service-dlq",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeDlq(NotificationEvent event) {
        log.error("[DLQ] Failed notification requires manual review: notificationId={}, tenantId={}, channel={}, recipient={}",
                event.notificationId(), event.tenantId(), event.channel(), event.recipient());
    }
}
