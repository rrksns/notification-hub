package com.notificationhub.common.event;

import java.time.Instant;

public record NotificationEvent(
        String notificationId,
        String tenantId,
        String channel,
        String recipient,
        String content,
        String idempotencyKey,
        Instant occurredAt
) {
    public static NotificationEvent of(
            String notificationId,
            String tenantId,
            String channel,
            String recipient,
            String content,
            String idempotencyKey
    ) {
        return new NotificationEvent(notificationId, tenantId, channel, recipient, content, idempotencyKey, Instant.now());
    }
}
