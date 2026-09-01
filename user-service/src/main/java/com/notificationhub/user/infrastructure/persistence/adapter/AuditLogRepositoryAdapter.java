// 감사 로그 출력 포트를 JPA 저장소로 연결하는 어댑터
package com.notificationhub.user.infrastructure.persistence.adapter;

import com.notificationhub.user.domain.model.AuditLog;
import com.notificationhub.user.domain.port.out.AuditLogRepository;
import com.notificationhub.user.infrastructure.persistence.mapper.AuditLogMapper;
import com.notificationhub.user.infrastructure.persistence.repository.AuditLogJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return AuditLogMapper.toDomain(jpaRepository.save(AuditLogMapper.toEntity(auditLog)));
    }
}
