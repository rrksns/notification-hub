// delivery 결과 outbox의 JPA 조회를 담당하는 repository
package com.notificationhub.delivery.infrastructure.persistence.repository;

import com.notificationhub.delivery.infrastructure.persistence.entity.DeliveryResultOutboxEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface DeliveryResultOutboxJpaRepository extends JpaRepository<DeliveryResultOutboxEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<DeliveryResultOutboxEntity> findByStatusOrderByCreatedAtAsc(
            DeliveryResultOutboxEntity.Status status, Pageable pageable);
}
