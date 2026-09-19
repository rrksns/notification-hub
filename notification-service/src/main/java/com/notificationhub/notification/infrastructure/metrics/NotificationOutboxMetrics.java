// notification outbox backlog과 Kafka 발행 실패를 Micrometer로 기록하는 컴포넌트
package com.notificationhub.notification.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import com.notificationhub.notification.domain.port.out.NotificationOutboxMetricsPort;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class NotificationOutboxMetrics implements NotificationOutboxMetricsPort {

    private final AtomicLong backlog = new AtomicLong();
    private final Counter publishFailureCounter;

    public NotificationOutboxMetrics(MeterRegistry registry) {
        Gauge.builder("notification.outbox.backlog", backlog, AtomicLong::get)
                .description("Current pending notification outbox entries")
                .register(registry);
        this.publishFailureCounter = Counter.builder("notification.outbox.publish.failure")
                .description("Total notification outbox publish failures")
                .register(registry);
    }

    @Override
    public void recordBacklog(long count) {
        backlog.set(count);
    }

    @Override
    public void incrementPublishFailure() {
        publishFailureCounter.increment();
    }
}
