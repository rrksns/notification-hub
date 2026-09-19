// delivery 결과 outbox 저장과 조회를 정의하는 포트
package com.notificationhub.delivery.domain.port.out;

import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;

import java.util.List;

public interface DeliveryResultOutboxRepository {
    DeliveryResultOutbox save(DeliveryResultOutbox outbox);
    List<DeliveryResultOutbox> findPending(int limit);

    long countPending();
}
