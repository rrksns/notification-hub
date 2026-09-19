// notification-service outbox dispatcher의 재시도 상태 전이를 검증하는 테스트
package com.notificationhub.notification.application;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationOutboxPort;
import com.notificationhub.notification.application.service.NotificationOutboxDispatcher;
import com.notificationhub.notification.infrastructure.metrics.NotificationOutboxMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxDispatcherTest {

    @Mock NotificationOutboxPort outboxPort;
    @Mock NotificationEventPublisher eventPublisher;

    NotificationOutboxDispatcher dispatcher;
    SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dispatcher = new NotificationOutboxDispatcher(outboxPort, eventPublisher,
                new NotificationOutboxMetrics(meterRegistry));
    }

    @Test
    void publishSuccess_marksOutboxPublished() {
        NotificationEvent event = event("notification-1");
        given(outboxPort.findPending()).willReturn(List.of(event));
        given(outboxPort.countPending()).willReturn(0L);

        dispatcher.dispatch();

        then(eventPublisher).should().publish(event);
        then(outboxPort).should().markPublished("notification-1");
        assertThat(meterRegistry.get("notification.outbox.backlog").gauge().value()).isZero();
    }

    @Test
    void publishFailure_keepsOutboxPending() {
        NotificationEvent event = event("notification-2");
        given(outboxPort.findPending()).willReturn(List.of(event));
        given(outboxPort.countPending()).willReturn(1L);
        willThrow(new RuntimeException("Kafka unavailable")).given(eventPublisher).publish(event);

        dispatcher.dispatch();

        then(outboxPort).should(never()).markPublished(anyString());
        assertThat(meterRegistry.get("notification.outbox.backlog").gauge().value()).isEqualTo(1);
        assertThat(meterRegistry.get("notification.outbox.publish.failure").counter().count()).isEqualTo(1);
    }

    private NotificationEvent event(String notificationId) {
        return NotificationEvent.of(notificationId, "tenant-1", "EMAIL", "user@test.com", "Hello", "key-" + notificationId);
    }
}
