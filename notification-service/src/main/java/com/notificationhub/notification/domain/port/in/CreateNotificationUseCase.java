package com.notificationhub.notification.domain.port.in;

public interface CreateNotificationUseCase {
    Result create(Command command);

    record Command(String tenantId, String channel, String recipient, String content, String idempotencyKey, String plan) {
        public Command(String tenantId, String channel, String recipient, String content, String idempotencyKey) {
            this(tenantId, channel, recipient, content, idempotencyKey, "FREE");
        }
    }
    record Result(String notificationId, String status) {}
}
