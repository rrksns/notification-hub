package com.notificationhub.common.event;

import java.time.Instant;

public record DeliveryResultEvent(
        String deliveryLogId,
        String notificationId,
        String tenantId,
        String channel,
        String status,
        String failureReason,
        Instant occurredAt
) {
    public static DeliveryResultEvent success(String deliveryLogId, String notificationId, String tenantId, String channel) {
        return new DeliveryResultEvent(deliveryLogId, notificationId, tenantId, channel, "SUCCESS", null, Instant.now());
    }

    public static DeliveryResultEvent failure(String deliveryLogId, String notificationId, String tenantId, String channel, String reason) {
        return new DeliveryResultEvent(deliveryLogId, notificationId, tenantId, channel, "FAILED", reason, Instant.now());
    }
}
