package com.notificationhub.delivery.presentation.dto;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryStatus;

import java.time.LocalDateTime;

public record DeliveryLogResponse(
        String id,
        String notificationId,
        String tenantId,
        ChannelType channel,
        String recipient,
        DeliveryStatus status,
        String failureReason,
        int attemptCount,
        LocalDateTime createdAt
) {
    public static DeliveryLogResponse from(DeliveryLog log) {
        return new DeliveryLogResponse(
                log.getId(),
                log.getNotificationId(),
                log.getTenantId(),
                log.getChannel(),
                log.getRecipient(),
                log.getStatus(),
                log.getFailureReason(),
                log.getAttemptCount(),
                log.getCreatedAt()
        );
    }
}
