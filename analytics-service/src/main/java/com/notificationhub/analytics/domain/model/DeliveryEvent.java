package com.notificationhub.analytics.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeliveryEvent {
    private final String id;
    private final String deliveryLogId;
    private final String notificationId;
    private final String tenantId;
    private final String channel;
    private final String status;
    private final String failureReason;
    private final LocalDateTime occurredAt;

    private DeliveryEvent(String id, String deliveryLogId, String notificationId, String tenantId,
                          String channel, String status, String failureReason, LocalDateTime occurredAt) {
        this.id = id;
        this.deliveryLogId = deliveryLogId;
        this.notificationId = notificationId;
        this.tenantId = tenantId;
        this.channel = channel;
        this.status = status;
        this.failureReason = failureReason;
        this.occurredAt = occurredAt;
    }

    public static DeliveryEvent create(String deliveryLogId, String notificationId, String tenantId,
                                       String channel, String status, String failureReason, LocalDateTime occurredAt) {
        return new DeliveryEvent(UUID.randomUUID().toString(), deliveryLogId, notificationId,
                tenantId, channel, status, failureReason, occurredAt);
    }

    public static DeliveryEvent reconstruct(String id, String deliveryLogId, String notificationId, String tenantId,
                                             String channel, String status, String failureReason, LocalDateTime occurredAt) {
        return new DeliveryEvent(id, deliveryLogId, notificationId, tenantId, channel, status, failureReason, occurredAt);
    }

    public boolean isSuccess() { return "SUCCESS".equals(status); }

    public String getId() { return id; }
    public String getDeliveryLogId() { return deliveryLogId; }
    public String getNotificationId() { return notificationId; }
    public String getTenantId() { return tenantId; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
