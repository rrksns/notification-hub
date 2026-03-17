package com.notificationhub.notification.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final Counter notificationSentCounter;
    private final Counter notificationDuplicateCounter;

    public NotificationMetrics(MeterRegistry registry) {
        this.notificationSentCounter = Counter.builder("notification.sent.total")
                .description("Total notifications sent")
                .register(registry);
        this.notificationDuplicateCounter = Counter.builder("notification.duplicate.total")
                .description("Total duplicate notification requests")
                .register(registry);
    }

    public void incrementSent() {
        notificationSentCounter.increment();
    }

    public void incrementDuplicate() {
        notificationDuplicateCounter.increment();
    }
}
