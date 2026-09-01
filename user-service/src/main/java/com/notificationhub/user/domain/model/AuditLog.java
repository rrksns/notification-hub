// 사용자 작업 감사 이벤트를 표현하는 불변 도메인 모델
package com.notificationhub.user.domain.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record AuditLog(
        String id,
        String tenantId,
        String actorId,
        String action,
        String resource,
        LocalDateTime occurredAt
) {
    public static AuditLog create(String tenantId, String actorId, String action, String resource) {
        return new AuditLog(UUID.randomUUID().toString(), tenantId, actorId, action, resource,
                LocalDateTime.now(ZoneOffset.UTC));
    }
}
