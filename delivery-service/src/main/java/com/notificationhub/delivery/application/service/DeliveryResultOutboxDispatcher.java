// pending delivery 결과 outbox를 Kafka로 발행하는 scheduled dispatcher
package com.notificationhub.delivery.application.service;

import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxMetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class DeliveryResultOutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(DeliveryResultOutboxDispatcher.class);
    private static final int BATCH_SIZE = 100;

    private final DeliveryResultOutboxRepository outboxRepository;
    private final DeliveryResultPublisher resultPublisher;
    private final DeliveryResultOutboxMetricsPort metrics;

    public DeliveryResultOutboxDispatcher(DeliveryResultOutboxRepository outboxRepository,
                                          DeliveryResultPublisher resultPublisher,
                                          DeliveryResultOutboxMetricsPort metrics) {
        this.outboxRepository = outboxRepository;
        this.resultPublisher = resultPublisher;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${delivery.result-outbox.poll-delay-ms:1000}")
    @Transactional
    public void dispatch() {
        try {
            outboxRepository.findPending(BATCH_SIZE).forEach(this::dispatchOne);
        } finally {
            metrics.recordBacklog(outboxRepository.countPending());
        }
    }

    private void dispatchOne(DeliveryResultOutbox outbox) {
        try {
            resultPublisher.publish(outbox);
            outboxRepository.save(outbox.markPublished(Instant.now()));
        } catch (RuntimeException e) {
            metrics.incrementPublishFailure();
            log.warn("Delivery result outbox publish failed: notificationId={}", outbox.notificationId(), e);
        }
    }
}
