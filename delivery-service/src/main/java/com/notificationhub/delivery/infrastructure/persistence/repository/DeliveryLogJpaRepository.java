package com.notificationhub.delivery.infrastructure.persistence.repository;

import com.notificationhub.delivery.infrastructure.persistence.entity.DeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryLogJpaRepository extends JpaRepository<DeliveryLogEntity, String> {
    List<DeliveryLogEntity> findByNotificationId(String notificationId);
    Optional<DeliveryLogEntity> findByIdAndTenantId(String id, String tenantId);
    List<DeliveryLogEntity> findByTenantId(String tenantId);
}
