package com.notificationhub.delivery.infrastructure.persistence.adapter;

import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.infrastructure.persistence.entity.DeliveryLogEntity;
import com.notificationhub.delivery.infrastructure.persistence.repository.DeliveryLogJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DeliveryLogRepositoryAdapter implements DeliveryLogRepository {

    private final DeliveryLogJpaRepository jpaRepository;

    public DeliveryLogRepositoryAdapter(DeliveryLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DeliveryLog save(DeliveryLog deliveryLog) {
        return jpaRepository.save(DeliveryLogEntity.from(deliveryLog)).toDomain();
    }

    @Override
    public Optional<DeliveryLog> findById(String id) {
        return jpaRepository.findById(id).map(DeliveryLogEntity::toDomain);
    }

    @Override
    public List<DeliveryLog> findByNotificationId(String notificationId) {
        return jpaRepository.findByNotificationId(notificationId)
                .stream().map(DeliveryLogEntity::toDomain).toList();
    }

    @Override
    public List<DeliveryLog> findByTenantId(String tenantId) {
        return jpaRepository.findByTenantId(tenantId)
                .stream().map(DeliveryLogEntity::toDomain).toList();
    }
}
