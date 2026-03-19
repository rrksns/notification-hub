package com.notificationhub.notification.domain.port.out;

public interface IdempotencyPort {
    boolean isDuplicate(String tenantId, String idempotencyKey);
    void save(String tenantId, String idempotencyKey);
}
