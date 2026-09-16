package com.notificationhub.delivery.application;

import com.notificationhub.delivery.application.service.ProcessDeliveryService;
import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryResultOutbox;
import com.notificationhub.delivery.domain.model.DeliveryStatus;
import com.notificationhub.delivery.domain.port.in.ProcessDeliveryUseCase;
import com.notificationhub.delivery.domain.port.out.ChannelDelivererPort;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProcessDeliveryServiceTest {

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @Mock
    private ChannelDelivererPort channelDelivererPort;

    @Mock
    private DeliveryResultOutboxRepository deliveryResultOutboxRepository;

    private ProcessDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new ProcessDeliveryService(deliveryLogRepository, channelDelivererPort, deliveryResultOutboxRepository);
    }

    @Test
    @DisplayName("EMAIL 발송 성공 시 DeliveryLog SUCCESS 저장 + 결과 outbox 저장")
    void process_emailSuccess_savesSuccessAndOutbox() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-1", "tenant-1", "EMAIL", "user@example.com", "Hello!", "idem-key-1"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(channelDelivererPort).deliver(any(), any(), any());
        ProcessDeliveryUseCase.Result result = service.process(command);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SUCCESS.name());
        assertThat(result.deliveryLogId()).isNotNull();

        then(channelDelivererPort).should().deliver(ChannelType.EMAIL, "user@example.com", "Hello!");
        then(deliveryResultOutboxRepository).should().save(any());
    }

    @Test
    @DisplayName("발송 실패 시 DeliveryLog FAILED 저장 + 실패 outbox 저장")
    void process_deliveryFails_savesFailedAndOutbox() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-2", "tenant-1", "SMS", "+821012345678", "OTP: 123456", "idem-key-2"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("Provider unavailable"))
                .given(channelDelivererPort).deliver(any(), any(), any());
        ProcessDeliveryUseCase.Result result = service.process(command);

        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED.name());

        ArgumentCaptor<DeliveryResultOutbox> captor = ArgumentCaptor.forClass(DeliveryResultOutbox.class);
        then(deliveryResultOutboxRepository).should().save(captor.capture());
        assertThat(captor.getValue().failureReason()).contains("Provider unavailable");
    }

    @Test
    @DisplayName("PUSH 채널 명령어가 PUSH 채널 deliverer를 호출")
    void process_pushChannel_callsPushDeliverer() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-3", "tenant-2", "PUSH", "device-token-xyz", "Push message", "idem-key-3"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(channelDelivererPort).deliver(any(), any(), any());
        service.process(command);

        then(channelDelivererPort).should().deliver(ChannelType.PUSH, "device-token-xyz", "Push message");
    }

    @Test
    @DisplayName("DeliveryLog 저장은 발송 전/후 총 2번 호출 (PENDING 저장 + 결과 저장)")
    void process_savesDeliveryLogTwice() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-4", "tenant-1", "EMAIL", "a@b.com", "content", "idem-key-4"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(channelDelivererPort).deliver(any(), any(), any());
        service.process(command);

        then(deliveryLogRepository).should(times(2)).save(any(DeliveryLog.class));
    }

    @Test
    void process_skipsExistingNotificationWithoutProviderOrOutboxWrite() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-duplicate", "tenant-1", "EMAIL", "a@b.com", "content", "idem-key-duplicate"
        );
        DeliveryLog existing = DeliveryLog.reconstruct(
                "delivery-existing", "notif-duplicate", "tenant-1", ChannelType.EMAIL, "a@b.com",
                DeliveryStatus.SUCCESS, null, 1, LocalDateTime.now()
        );
        given(deliveryLogRepository.findByNotificationId("notif-duplicate")).willReturn(List.of(existing));

        ProcessDeliveryUseCase.Result result = service.process(command);

        assertThat(result.deliveryLogId()).isEqualTo("delivery-existing");
        assertThat(result.status()).isEqualTo(DeliveryStatus.SUCCESS.name());
        then(channelDelivererPort).shouldHaveNoInteractions();
        then(deliveryResultOutboxRepository).shouldHaveNoInteractions();
    }
}
