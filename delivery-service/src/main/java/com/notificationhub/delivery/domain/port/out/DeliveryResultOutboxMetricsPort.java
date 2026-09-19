// delivery result outbox 관측성 지표 기록을 정의하는 출력 포트
package com.notificationhub.delivery.domain.port.out;

public interface DeliveryResultOutboxMetricsPort {
    void recordBacklog(long count);

    void incrementPublishFailure();
}
