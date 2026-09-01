// 감사 로그를 영속화하는 출력 포트
package com.notificationhub.user.domain.port.out;

import com.notificationhub.user.domain.model.AuditLog;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);
}
