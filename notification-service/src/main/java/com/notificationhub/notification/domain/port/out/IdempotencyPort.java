package com.notificationhub.notification.domain.port.out;

public interface IdempotencyPort {
    boolean isDuplicate(String idempotencyKey);
    void save(String idempotencyKey);
}
