// 테넌트별 알림 월간 쿼터 소비를 담당하는 출력 포트
package com.notificationhub.notification.domain.port.out;

public interface NotificationQuotaPort {
    boolean tryConsume(String tenantId, String plan);
}
