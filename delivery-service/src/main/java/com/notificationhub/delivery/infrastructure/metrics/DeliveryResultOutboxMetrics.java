// delivery result outbox backlog과 Kafka 발행 실패를 Micrometer로 기록하는 컴포넌트
package com.notificationhub.delivery.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxMetricsPort;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class DeliveryResultOutboxMetrics implements DeliveryResultOutboxMetricsPort {

    private final AtomicLong backlog = new AtomicLong();
    private final Counter publishFailureCounter;

    public DeliveryResultOutboxMetrics(MeterRegistry registry) {
        Gauge.builder("delivery.result.outbox.backlog", backlog, AtomicLong::get)
                .description("Current pending delivery result outbox entries")
                .register(registry);
        this.publishFailureCounter = Counter.builder("delivery.result.outbox.publish.failure")
                .description("Total delivery result outbox publish failures")
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
