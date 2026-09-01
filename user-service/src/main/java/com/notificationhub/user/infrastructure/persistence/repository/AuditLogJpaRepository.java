// 감사 로그 JPA 저장소
package com.notificationhub.user.infrastructure.persistence.repository;

import com.notificationhub.user.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, String> {
}
