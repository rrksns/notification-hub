// 알림 outbox 저장과 pending 이벤트 조회를 정의하는 출력 포트
package com.notificationhub.notification.domain.port.out;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.model.Notification;

import java.util.List;
import java.time.Instant;

public interface NotificationOutboxPort {
    void save(Notification notification);

    List<NotificationEvent> findPending();

    long countPending();

    void markPublished(String notificationId);

    int deletePublishedBefore(Instant cutoff);
}
