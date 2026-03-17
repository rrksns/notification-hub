package com.notificationhub.analytics.infrastructure.persistence.document;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "delivery_events")
public class DeliveryEventDocument {

    @Id
    private String id;
    private String deliveryLogId;
    private String notificationId;
    private String tenantId;
    private String channel;
    private String status;
    private String failureReason;
    private LocalDateTime occurredAt;

    public static DeliveryEventDocument from(DeliveryEvent event) {
        DeliveryEventDocument doc = new DeliveryEventDocument();
        doc.id = event.getId();
        doc.deliveryLogId = event.getDeliveryLogId();
        doc.notificationId = event.getNotificationId();
        doc.tenantId = event.getTenantId();
        doc.channel = event.getChannel();
        doc.status = event.getStatus();
        doc.failureReason = event.getFailureReason();
        doc.occurredAt = event.getOccurredAt();
        return doc;
    }

    public DeliveryEvent toDomain() {
        return DeliveryEvent.reconstruct(id, deliveryLogId, notificationId, tenantId,
                channel, status, failureReason, occurredAt);
    }

    public String getTenantId() { return tenantId; }
}
