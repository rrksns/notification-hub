package com.notificationhub.notification.domain.model;

import com.notificationhub.notification.domain.exception.InvalidNotificationException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {
    private final String id;
    private final String tenantId;
    private final Channel channel;
    private final String recipient;
    private final String content;
    private final String idempotencyKey;
    private NotificationStatus status;
    private final LocalDateTime createdAt;

    private Notification(String id, String tenantId, Channel channel, String recipient,
                         String content, String idempotencyKey, NotificationStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.channel = channel;
        this.recipient = recipient;
        this.content = content;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Notification create(String tenantId, Channel channel, String recipient, String content, String idempotencyKey) {
        if (recipient == null || recipient.isBlank()) throw new InvalidNotificationException("Recipient is required");
        if (content == null || content.isBlank()) throw new InvalidNotificationException("Content is required");
        return new Notification(UUID.randomUUID().toString(), tenantId, channel, recipient,
                content, idempotencyKey, NotificationStatus.PENDING, LocalDateTime.now());
    }

    public static Notification reconstruct(String id, String tenantId, Channel channel, String recipient,
                                            String content, String idempotencyKey, NotificationStatus status, LocalDateTime createdAt) {
        return new Notification(id, tenantId, channel, recipient, content, idempotencyKey, status, createdAt);
    }

    public void publish() {
        if (this.status != NotificationStatus.PENDING) {
            throw new InvalidNotificationException("Only PENDING notifications can be published, current: " + this.status);
        }
        this.status = NotificationStatus.PUBLISHED;
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
