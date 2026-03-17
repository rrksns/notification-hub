package com.notificationhub.delivery.infrastructure.persistence.entity;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_logs")
public class DeliveryLogEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String notificationId;

    @Column(nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType channel;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column
    private String failureReason;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected DeliveryLogEntity() {}

    public static DeliveryLogEntity from(DeliveryLog log) {
        DeliveryLogEntity entity = new DeliveryLogEntity();
        entity.id = log.getId();
        entity.notificationId = log.getNotificationId();
        entity.tenantId = log.getTenantId();
        entity.channel = log.getChannel();
        entity.recipient = log.getRecipient();
        entity.status = log.getStatus();
        entity.failureReason = log.getFailureReason();
        entity.attemptCount = log.getAttemptCount();
        entity.createdAt = log.getCreatedAt();
        return entity;
    }

    public DeliveryLog toDomain() {
        return DeliveryLog.reconstruct(
                id, notificationId, tenantId, channel, recipient,
                status, failureReason, attemptCount, createdAt
        );
    }

    public String getId() { return id; }
    public String getNotificationId() { return notificationId; }
    public String getTenantId() { return tenantId; }
    public ChannelType getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public DeliveryStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
