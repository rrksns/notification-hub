package com.notificationhub.delivery.domain.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class DeliveryLog {

    private final String id;
    private final String notificationId;
    private final String tenantId;
    private final ChannelType channel;
    private final String recipient;
    private final DeliveryStatus status;
    private final String failureReason;
    private final int attemptCount;
    private final LocalDateTime createdAt;

    private DeliveryLog(String id, String notificationId, String tenantId,
                        ChannelType channel, String recipient,
                        DeliveryStatus status, String failureReason,
                        int attemptCount, LocalDateTime createdAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.tenantId = tenantId;
        this.channel = channel;
        this.recipient = recipient;
        this.status = status;
        this.failureReason = failureReason;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
    }

    public static DeliveryLog create(String notificationId, String tenantId,
                                     ChannelType channel, String recipient) {
        if (notificationId == null || notificationId.isBlank()) {
            throw new IllegalArgumentException("notificationId is required");
        }
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
        return new DeliveryLog(
                UUID.randomUUID().toString(),
                notificationId, tenantId, channel, recipient,
                DeliveryStatus.PENDING, null, 0, LocalDateTime.now(ZoneOffset.UTC)
        );
    }

    public static DeliveryLog reconstruct(String id, String notificationId, String tenantId,
                                          ChannelType channel, String recipient,
                                          DeliveryStatus status, String failureReason,
                                          int attemptCount, LocalDateTime createdAt) {
        return new DeliveryLog(id, notificationId, tenantId, channel, recipient,
                status, failureReason, attemptCount, createdAt);
    }

    public DeliveryLog markSuccess() {
        if (this.status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Cannot mark SUCCESS from status: " + this.status);
        }
        return new DeliveryLog(id, notificationId, tenantId, channel, recipient,
                DeliveryStatus.SUCCESS, failureReason, attemptCount + 1, createdAt);
    }

    public DeliveryLog markFailed(String reason) {
        if (this.status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Cannot mark FAILED from status: " + this.status);
        }
        return new DeliveryLog(id, notificationId, tenantId, channel, recipient,
                DeliveryStatus.FAILED, reason, attemptCount + 1, createdAt);
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
