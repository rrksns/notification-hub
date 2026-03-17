package com.notificationhub.notification.infrastructure.persistence.entity;

import com.notificationhub.notification.domain.model.Channel;
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

    public NotificationEntity(String id, String tenantId, Channel channel, String recipient,
                               String content, String idempotencyKey, NotificationStatus status,
                               LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.channel = channel;
        this.recipient = recipient;
        this.content = content;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public Channel getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getContent() { return content; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public NotificationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
