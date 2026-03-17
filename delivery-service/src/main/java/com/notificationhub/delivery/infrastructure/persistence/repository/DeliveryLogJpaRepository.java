package com.notificationhub.delivery.infrastructure.persistence.repository;

import com.notificationhub.delivery.infrastructure.persistence.entity.DeliveryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryLogJpaRepository extends JpaRepository<DeliveryLogEntity, String> {
    List<DeliveryLogEntity> findByNotificationId(String notificationId);
    List<DeliveryLogEntity> findByTenantId(String tenantId);
}
