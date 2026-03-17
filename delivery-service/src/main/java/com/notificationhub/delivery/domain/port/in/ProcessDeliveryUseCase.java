package com.notificationhub.delivery.domain.port.in;

public interface ProcessDeliveryUseCase {

    Result process(Command command);

    record Command(
            String notificationId,
            String tenantId,
            String channel,
            String recipient,
            String content,
            String idempotencyKey
    ) {}

    record Result(
            String deliveryLogId,
            String status
    ) {}
}
