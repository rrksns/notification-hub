package com.notificationhub.notification.infrastructure.persistence.adapter;

import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import com.notificationhub.notification.infrastructure.persistence.mapper.NotificationMapper;
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
        return NotificationMapper.toDomain(jpaRepository.save(NotificationMapper.toEntity(notification)));
    }

    @Override
    public Optional<Notification> findById(String id) {
        return jpaRepository.findById(id).map(NotificationMapper::toDomain);
    }

    @Override
    public Optional<Notification> findByIdAndTenantId(String id, String tenantId) {
        return jpaRepository.findByIdAndTenantId(id, tenantId).map(NotificationMapper::toDomain);
    }
}
