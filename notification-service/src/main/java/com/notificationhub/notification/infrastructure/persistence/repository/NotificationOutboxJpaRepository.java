// notification outbox의 pending 이벤트 조회와 발행 완료 처리를 담당하는 저장소
package com.notificationhub.notification.infrastructure.persistence.repository;

import com.notificationhub.notification.infrastructure.persistence.entity.NotificationOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface NotificationOutboxJpaRepository extends JpaRepository<NotificationOutboxEntity, String> {
    List<NotificationOutboxEntity> findTop100ByStatusOrderByOccurredAtAsc(NotificationOutboxEntity.Status status);

    @Modifying
    @Transactional
    @Query("update NotificationOutboxEntity e set e.status = :publishedStatus, e.publishedAt = :publishedAt where e.notificationId = :notificationId and e.status = :pendingStatus")
    int markPublished(@Param("notificationId") String notificationId,
                      @Param("publishedAt") Instant publishedAt,
                      @Param("pendingStatus") NotificationOutboxEntity.Status pendingStatus,
                      @Param("publishedStatus") NotificationOutboxEntity.Status publishedStatus);
}
