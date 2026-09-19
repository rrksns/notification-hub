// pending notification outbox 이벤트를 Kafka로 재발행하는 스케줄러
package com.notificationhub.notification.application.service;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationOutboxPort;
import com.notificationhub.notification.domain.port.out.NotificationOutboxMetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDispatcher.class);

    private final NotificationOutboxPort outboxPort;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationOutboxMetricsPort metrics;

    public NotificationOutboxDispatcher(NotificationOutboxPort outboxPort,
                                        NotificationEventPublisher eventPublisher,
                                        NotificationOutboxMetricsPort metrics) {
        this.outboxPort = outboxPort;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${notification.outbox-poll-delay-ms:1000}")
    @Transactional
    public void dispatch() {
        try {
            outboxPort.findPending().forEach(this::dispatchOne);
        } finally {
            metrics.recordBacklog(outboxPort.countPending());
        }
    }

    private void dispatchOne(NotificationEvent event) {
        try {
            eventPublisher.publish(event);
            outboxPort.markPublished(event.notificationId());
        } catch (RuntimeException e) {
            metrics.incrementPublishFailure();
            log.warn("Notification outbox publish failed: notificationId={}", event.notificationId(), e);
        }
    }
}
