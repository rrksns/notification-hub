package com.notificationhub.delivery.application;

import com.notificationhub.delivery.application.service.ProcessDeliveryService;
import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryStatus;
import com.notificationhub.delivery.domain.port.in.ProcessDeliveryUseCase;
import com.notificationhub.delivery.domain.port.out.ChannelDelivererPort;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import com.notificationhub.delivery.domain.port.out.DeliveryResultPublisher;
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

@ExtendWith(MockitoExtension.class)
class ProcessDeliveryServiceTest {

    @Mock
    private DeliveryLogRepository deliveryLogRepository;

    @Mock
    private ChannelDelivererPort channelDelivererPort;

    @Mock
    private DeliveryResultPublisher deliveryResultPublisher;

    private ProcessDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new ProcessDeliveryService(deliveryLogRepository, channelDelivererPort, deliveryResultPublisher);
    }

    @Test
    @DisplayName("EMAIL 발송 성공 시 DeliveryLog SUCCESS 저장 + 결과 이벤트 발행")
    void process_emailSuccess_savesSuccessAndPublishes() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-1", "tenant-1", "EMAIL", "user@example.com", "Hello!", "idem-key-1"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(channelDelivererPort).deliver(any(), any(), any());
        willDoNothing().given(deliveryResultPublisher).publishSuccess(any());

        ProcessDeliveryUseCase.Result result = service.process(command);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SUCCESS.name());
        assertThat(result.deliveryLogId()).isNotNull();

        then(channelDelivererPort).should().deliver(ChannelType.EMAIL, "user@example.com", "Hello!");
        then(deliveryResultPublisher).should().publishSuccess(any(DeliveryLog.class));
        then(deliveryResultPublisher).should(never()).publishFailure(any(DeliveryLog.class));
    }

    @Test
    @DisplayName("발송 실패 시 DeliveryLog FAILED 저장 + 실패 이벤트 발행")
    void process_deliveryFails_savesFailedAndPublishes() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-2", "tenant-1", "SMS", "+821012345678", "OTP: 123456", "idem-key-2"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willThrow(new RuntimeException("Provider unavailable"))
                .given(channelDelivererPort).deliver(any(), any(), any());
        willDoNothing().given(deliveryResultPublisher).publishFailure(any());

        ProcessDeliveryUseCase.Result result = service.process(command);

        assertThat(result.status()).isEqualTo(DeliveryStatus.FAILED.name());

        ArgumentCaptor<DeliveryLog> captor = ArgumentCaptor.forClass(DeliveryLog.class);
        then(deliveryResultPublisher).should().publishFailure(captor.capture());
        assertThat(captor.getValue().getFailureReason()).contains("Provider unavailable");
    }

    @Test
    @DisplayName("PUSH 채널 명령어가 PUSH 채널 deliverer를 호출")
    void process_pushChannel_callsPushDeliverer() {
        ProcessDeliveryUseCase.Command command = new ProcessDeliveryUseCase.Command(
                "notif-3", "tenant-2", "PUSH", "device-token-xyz", "Push message", "idem-key-3"
        );

        given(deliveryLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        willDoNothing().given(channelDelivererPort).deliver(any(), any(), any());
        willDoNothing().given(deliveryResultPublisher).publishSuccess(any());

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
        willDoNothing().given(deliveryResultPublisher).publishSuccess(any());

        service.process(command);

        then(deliveryLogRepository).should(times(2)).save(any(DeliveryLog.class));
    }
}
