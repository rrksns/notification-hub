package com.notificationhub.notification.infrastructure.persistence.adapter;

import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import com.notificationhub.notification.infrastructure.persistence.entity.NotificationEntity;
import com.notificationhub.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        return jpaRepository.save(NotificationEntity.from(notification)).toDomain();
    }

    @Override
    public Optional<Notification> findById(String id) {
        return jpaRepository.findById(id).map(NotificationEntity::toDomain);
    }

    @Override
    public Optional<Notification> findByIdAndTenantId(String id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(NotificationEntity::toDomain);
    }
}
