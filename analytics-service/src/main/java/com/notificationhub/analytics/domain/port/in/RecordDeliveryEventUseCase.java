package com.notificationhub.analytics.domain.port.in;

import java.time.LocalDateTime;

public interface RecordDeliveryEventUseCase {
    void record(Command command);

    record Command(
            String deliveryLogId,
            String notificationId,
            String tenantId,
            String channel,
            String status,
            String failureReason,
            LocalDateTime occurredAt
    ) {}
}
