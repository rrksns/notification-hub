package com.notificationhub.notification.infrastructure.persistence.entity;

import com.notificationhub.notification.domain.model.Channel;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.model.NotificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tenantId;
    @Enumerated(EnumType.STRING)
    private Channel channel;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private LocalDateTime createdAt;

    protected NotificationEntity() {}

    public static NotificationEntity from(Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.id = n.getId();
        e.tenantId = n.getTenantId();
        e.channel = n.getChannel();
        e.recipient = n.getRecipient();
        e.content = n.getContent();
        e.idempotencyKey = n.getIdempotencyKey();
        e.status = n.getStatus();
        e.createdAt = n.getCreatedAt();
        return e;
    }

    public Notification toDomain() {
        return Notification.reconstruct(id, tenantId, channel, recipient, content, idempotencyKey, status, createdAt);
    }
}
