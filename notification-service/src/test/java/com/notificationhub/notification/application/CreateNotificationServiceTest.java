package com.notificationhub.notification.application;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.notification.application.service.CreateNotificationService;
import com.notificationhub.notification.domain.model.Channel;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.domain.port.out.IdempotencyPort;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CreateNotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock IdempotencyPort idempotencyPort;
    @Mock NotificationEventPublisher eventPublisher;

    CreateNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateNotificationService(notificationRepository, idempotencyPort, eventPublisher);
    }

    @Test
    @DisplayName("정상 알림 생성 — 저장 + Kafka 이벤트 발행")
    void create_success() {
        given(idempotencyPort.isDuplicate("key-001")).willReturn(false);
        given(notificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateNotificationUseCase.Command cmd = new CreateNotificationUseCase.Command(
                "tenant-1", "EMAIL", "user@test.com", "Hello", "key-001"
        );
        CreateNotificationUseCase.Result result = useCase.create(cmd);

        assertThat(result.notificationId()).isNotBlank();
        then(idempotencyPort).should().save("key-001");
        then(eventPublisher).should().publish(any(Notification.class));
    }

    @Test
    @DisplayName("동일 idempotencyKey 재요청 시 예외 발생")
    void create_duplicateKey_throws() {
        given(idempotencyPort.isDuplicate("key-001")).willReturn(true);

        CreateNotificationUseCase.Command cmd = new CreateNotificationUseCase.Command(
                "tenant-1", "EMAIL", "user@test.com", "Hello", "key-001"
        );

        assertThatThrownBy(() -> useCase.create(cmd))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("알림 저장 후 상태가 PUBLISHED로 변경됨")
    void create_statusBecomesPublished() {
        given(idempotencyPort.isDuplicate("key-002")).willReturn(false);
        given(notificationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateNotificationUseCase.Command cmd = new CreateNotificationUseCase.Command(
                "tenant-1", "SMS", "010-1234-5678", "Hi", "key-002"
        );
        useCase.create(cmd);

        then(notificationRepository).should().save(argThat(n ->
                n.getStatus().name().equals("PUBLISHED")
        ));
    }
}
