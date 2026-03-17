package com.notificationhub.delivery.domain.port.out;

import com.notificationhub.delivery.domain.model.DeliveryLog;

public interface DeliveryResultPublisher {
    void publishSuccess(DeliveryLog deliveryLog);
    void publishFailure(DeliveryLog deliveryLog);
}
