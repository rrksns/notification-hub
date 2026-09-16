// delivery 결과 outbox를 저장하는 JPA entity
package com.notificationhub.delivery.infrastructure.persistence.entity;

import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "delivery_result_outbox")
public class DeliveryResultOutboxEntity {
    public enum Status { PENDING, PUBLISHED }

    @Id
    private String id;
    @Column(nullable = false) private String deliveryLogId;
    @Column(nullable = false) private String notificationId;
    @Column(nullable = false) private String tenantId;
    @Column(nullable = false) private String channel;
    @Column(nullable = false) private String resultStatus;
    @Column private String failureReason;
    @Column(nullable = false) private Instant createdAt;
    @Column private Instant publishedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private Status status;

    protected DeliveryResultOutboxEntity() {}

    public static DeliveryResultOutboxEntity from(DeliveryResultOutbox outbox) {
        DeliveryResultOutboxEntity entity = new DeliveryResultOutboxEntity();
        entity.id = outbox.id();
        entity.deliveryLogId = outbox.deliveryLogId();
        entity.notificationId = outbox.notificationId();
        entity.tenantId = outbox.tenantId();
        entity.channel = outbox.channel();
        entity.resultStatus = outbox.status();
        entity.failureReason = outbox.failureReason();
        entity.createdAt = outbox.createdAt();
        entity.publishedAt = outbox.publishedAt();
        entity.status = outbox.isPublished() ? Status.PUBLISHED : Status.PENDING;
        return entity;
    }

    public DeliveryResultOutbox toDomain() {
        return new DeliveryResultOutbox(id, deliveryLogId, notificationId, tenantId, channel,
                resultStatus, failureReason, createdAt, publishedAt);
    }

    public void updateFrom(DeliveryResultOutbox outbox) {
        this.publishedAt = outbox.publishedAt();
        this.status = outbox.isPublished() ? Status.PUBLISHED : Status.PENDING;
    }
}
