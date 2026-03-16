package com.notificationhub.notification.infrastructure.persistence.repository;

import com.notificationhub.notification.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
    Optional<NotificationEntity> findByIdAndTenantId(String id, String tenantId);
}
