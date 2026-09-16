// delivery 결과 이벤트를 재발행하기 위한 outbox 도메인 모델
package com.notificationhub.delivery.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResultOutbox(
        String id,
        String deliveryLogId,
        String notificationId,
        String tenantId,
        String channel,
        String status,
        String failureReason,
        Instant createdAt,
        Instant publishedAt
) {
    public static DeliveryResultOutbox from(DeliveryLog deliveryLog) {
        return new DeliveryResultOutbox(
                UUID.randomUUID().toString(), deliveryLog.getId(), deliveryLog.getNotificationId(),
                deliveryLog.getTenantId(), deliveryLog.getChannel().name(), deliveryLog.getStatus().name(),
                deliveryLog.getFailureReason(), Instant.now(), null
        );
    }

    public DeliveryResultOutbox markPublished(Instant publishedAt) {
        return new DeliveryResultOutbox(
                id, deliveryLogId, notificationId, tenantId, channel, status, failureReason, createdAt, publishedAt
        );
    }

    public boolean isPublished() {
        return publishedAt != null;
    }
}
