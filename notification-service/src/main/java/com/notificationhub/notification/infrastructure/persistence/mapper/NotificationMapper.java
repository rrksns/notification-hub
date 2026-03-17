package com.notificationhub.notification.infrastructure.persistence.mapper;

import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.infrastructure.persistence.entity.NotificationEntity;

public class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationEntity toEntity(Notification notification) {
        return new NotificationEntity(
                notification.getId(),
                notification.getTenantId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getContent(),
                notification.getIdempotencyKey(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }

    public static Notification toDomain(NotificationEntity entity) {
        return Notification.reconstruct(
                entity.getId(),
                entity.getTenantId(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getContent(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
