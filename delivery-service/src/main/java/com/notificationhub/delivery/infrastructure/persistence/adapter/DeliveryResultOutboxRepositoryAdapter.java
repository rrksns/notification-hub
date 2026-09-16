// delivery 결과 outbox 도메인과 JPA entity를 연결하는 adapter
package com.notificationhub.delivery.infrastructure.persistence.adapter;

import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxRepository;
import com.notificationhub.delivery.infrastructure.persistence.entity.DeliveryResultOutboxEntity;
import com.notificationhub.delivery.infrastructure.persistence.repository.DeliveryResultOutboxJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeliveryResultOutboxRepositoryAdapter implements DeliveryResultOutboxRepository {
    private final DeliveryResultOutboxJpaRepository jpaRepository;

    public DeliveryResultOutboxRepositoryAdapter(DeliveryResultOutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DeliveryResultOutbox save(DeliveryResultOutbox outbox) {
        return jpaRepository.findById(outbox.id())
                .map(entity -> {
                    entity.updateFrom(outbox);
                    return jpaRepository.save(entity).toDomain();
                })
                .orElseGet(() -> jpaRepository.save(DeliveryResultOutboxEntity.from(outbox)).toDomain());
    }

    @Override
    public List<DeliveryResultOutbox> findPending(int limit) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(
                        DeliveryResultOutboxEntity.Status.PENDING, PageRequest.of(0, limit))
                .stream().map(DeliveryResultOutboxEntity::toDomain).toList();
    }
}
