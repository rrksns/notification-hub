// notification outbox JPA 저장소를 도메인 출력 포트로 연결하는 어댑터
package com.notificationhub.notification.infrastructure.persistence.adapter;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.out.NotificationOutboxPort;
import com.notificationhub.notification.infrastructure.persistence.entity.NotificationOutboxEntity;
import com.notificationhub.notification.infrastructure.persistence.repository.NotificationOutboxJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class NotificationOutboxRepositoryAdapter implements NotificationOutboxPort {

    private final NotificationOutboxJpaRepository jpaRepository;

    public NotificationOutboxRepositoryAdapter(NotificationOutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Notification notification) {
        jpaRepository.save(new NotificationOutboxEntity(notification));
    }

    @Override
    public List<NotificationEvent> findPending() {
        return jpaRepository.findTop100ByStatusOrderByOccurredAtAsc(NotificationOutboxEntity.Status.PENDING)
                .stream()
                .map(NotificationOutboxEntity::toEvent)
                .toList();
    }

    @Override
    public void markPublished(String notificationId) {
        jpaRepository.markPublished(
                notificationId,
                Instant.now(),
                NotificationOutboxEntity.Status.PENDING,
                NotificationOutboxEntity.Status.PUBLISHED
        );
    }
}
