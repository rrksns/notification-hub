package com.notificationhub.notification.domain.port.out;

/**
 * 알림 생성 유스케이스에서 기록하는 애플리케이션 메트릭(아웃 포트).
 * Micrometer 등 구체 구현은 infrastructure에 둔다.
 */
public interface NotificationApplicationMetrics {

    void incrementSent();

    void incrementDuplicate();
}
