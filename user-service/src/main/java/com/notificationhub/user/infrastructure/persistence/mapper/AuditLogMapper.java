// 감사 로그 도메인 모델과 JPA 엔티티를 변환하는 매퍼
package com.notificationhub.user.infrastructure.persistence.mapper;

import com.notificationhub.user.domain.model.AuditLog;
import com.notificationhub.user.infrastructure.persistence.entity.AuditLogEntity;

public final class AuditLogMapper {

    private AuditLogMapper() {}

    public static AuditLogEntity toEntity(AuditLog auditLog) {
        return new AuditLogEntity(auditLog.id(), auditLog.tenantId(), auditLog.actorId(), auditLog.action(),
                auditLog.resource(), auditLog.occurredAt());
    }

    public static AuditLog toDomain(AuditLogEntity entity) {
        return new AuditLog(entity.getId(), entity.getTenantId(), entity.getActorId(), entity.getAction(),
                entity.getResource(), entity.getOccurredAt());
    }
}
