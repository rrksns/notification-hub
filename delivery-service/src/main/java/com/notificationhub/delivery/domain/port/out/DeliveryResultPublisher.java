package com.notificationhub.delivery.domain.port.out;

import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;

public interface DeliveryResultPublisher {
    void publish(DeliveryResultOutbox outbox);
}
