package com.notificationhub.delivery.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeliveryLog {

    private final String id;
    private final String notificationId;
    private final String tenantId;
    private final ChannelType channel;
    private final String recipient;
    private DeliveryStatus status;
    private String failureReason;
    private int attemptCount;
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
                DeliveryStatus.PENDING, null, 0, LocalDateTime.now()
        );
    }

    public static DeliveryLog reconstruct(String id, String notificationId, String tenantId,
                                          ChannelType channel, String recipient,
                                          DeliveryStatus status, String failureReason,
                                          int attemptCount, LocalDateTime createdAt) {
        return new DeliveryLog(id, notificationId, tenantId, channel, recipient,
                status, failureReason, attemptCount, createdAt);
    }

    public void markSuccess() {
        if (this.status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Cannot mark SUCCESS from status: " + this.status);
        }
        this.status = DeliveryStatus.SUCCESS;
        this.attemptCount++;
    }

    public void markFailed(String reason) {
        if (this.status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Cannot mark FAILED from status: " + this.status);
        }
        this.status = DeliveryStatus.FAILED;
        this.failureReason = reason;
        this.attemptCount++;
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
