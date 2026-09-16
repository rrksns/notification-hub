// delivery 결과 outbox dispatcher의 발행과 재시도 상태를 검증하는 테스트
package com.notificationhub.delivery.application;

import com.notificationhub.delivery.application.service.DeliveryResultOutboxDispatcher;
import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryResultOutboxDispatcherTest {

    @Mock
    private DeliveryResultOutboxRepository outboxRepository;

    @Mock
    private DeliveryResultPublisher resultPublisher;

    private DeliveryResultOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new DeliveryResultOutboxDispatcher(outboxRepository, resultPublisher);
    }

    @Test
    void dispatch_publishesPendingOutboxAndMarksPublished() {
        DeliveryResultOutbox outbox = outbox();
        given(outboxRepository.findPending(100)).willReturn(List.of(outbox));

        dispatcher.dispatch();

        then(resultPublisher).should().publish(outbox);
        ArgumentCaptor<DeliveryResultOutbox> captor = ArgumentCaptor.forClass(DeliveryResultOutbox.class);
        then(outboxRepository).should().save(captor.capture());
        assertThat(captor.getValue().isPublished()).isTrue();
    }

    @Test
    void dispatch_keepsPendingOutboxWhenPublishingFails() {
        DeliveryResultOutbox outbox = outbox();
        given(outboxRepository.findPending(100)).willReturn(List.of(outbox));
        willThrow(new IllegalStateException("Kafka unavailable")).given(resultPublisher).publish(outbox);

        dispatcher.dispatch();

        then(outboxRepository).should(never()).save(any());
    }

    private DeliveryResultOutbox outbox() {
        return new DeliveryResultOutbox(
                "outbox-1", "delivery-1", "notification-1", "tenant-1", "EMAIL", "SUCCESS", null,
                Instant.parse("2026-09-17T00:00:00Z"), null
        );
    }
}
