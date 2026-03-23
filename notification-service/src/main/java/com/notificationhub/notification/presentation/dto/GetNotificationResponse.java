package com.notificationhub.notification.presentation.dto;

import com.notificationhub.notification.domain.model.Notification;

import java.time.LocalDateTime;

public record GetNotificationResponse(
        String id,
        String tenantId,
        String channel,
        String recipient,
        String content,
        String status,
        LocalDateTime createdAt
) {
    public static GetNotificationResponse from(Notification notification) {
        return new GetNotificationResponse(
                notification.getId(),
                notification.getTenantId(),
                notification.getChannel().name(),
                notification.getRecipient(),
                notification.getContent(),
                notification.getStatus().name(),
                notification.getCreatedAt()
        );
    }
}
