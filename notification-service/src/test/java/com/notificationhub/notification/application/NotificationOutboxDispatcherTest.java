// notification-service outbox dispatcher의 재시도 상태 전이를 검증하는 테스트
package com.notificationhub.notification.application;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationOutboxPort;
import com.notificationhub.notification.application.service.NotificationOutboxDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxDispatcherTest {

    @Mock NotificationOutboxPort outboxPort;
    @Mock NotificationEventPublisher eventPublisher;

    NotificationOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationOutboxDispatcher(outboxPort, eventPublisher);
    }

    @Test
    void publishSuccess_marksOutboxPublished() {
        NotificationEvent event = event("notification-1");
        given(outboxPort.findPending()).willReturn(List.of(event));

        dispatcher.dispatch();

        then(eventPublisher).should().publish(event);
        then(outboxPort).should().markPublished("notification-1");
    }

    @Test
    void publishFailure_keepsOutboxPending() {
        NotificationEvent event = event("notification-2");
        given(outboxPort.findPending()).willReturn(List.of(event));
        willThrow(new RuntimeException("Kafka unavailable")).given(eventPublisher).publish(event);

        dispatcher.dispatch();

        then(outboxPort).should(never()).markPublished(anyString());
    }

    private NotificationEvent event(String notificationId) {
        return NotificationEvent.of(notificationId, "tenant-1", "EMAIL", "user@test.com", "Hello", "key-" + notificationId);
    }
}
