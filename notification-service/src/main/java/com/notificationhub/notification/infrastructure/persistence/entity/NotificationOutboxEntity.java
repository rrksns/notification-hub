// notification Kafka 이벤트의 재발행 상태를 저장하는 JPA 엔티티
package com.notificationhub.notification.infrastructure.persistence.entity;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.model.Notification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.ZoneOffset;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutboxEntity {

    public enum Status {
        PENDING, PUBLISHED
    }

    @Id
    private String notificationId;
    @Column(nullable = false)
    private String tenantId;
    @Column(nullable = false)
    private String channel;
    @Column(nullable = false)
    private String recipient;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(nullable = false)
    private String idempotencyKey;
    @Column(nullable = false)
    private Instant occurredAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    private Instant publishedAt;

    protected NotificationOutboxEntity() {}

    public NotificationOutboxEntity(Notification notification) {
        this.notificationId = notification.getId();
        this.tenantId = notification.getTenantId();
        this.channel = notification.getChannel().name();
        this.recipient = notification.getRecipient();
        this.content = notification.getContent();
        this.idempotencyKey = notification.getIdempotencyKey();
        this.occurredAt = notification.getCreatedAt().toInstant(ZoneOffset.UTC);
        this.status = Status.PENDING;
    }

    public NotificationEvent toEvent() {
        return new NotificationEvent(notificationId, tenantId, channel, recipient, content, idempotencyKey, occurredAt);
    }
}
