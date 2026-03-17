package com.notificationhub.analytics.application;

import com.notificationhub.analytics.application.service.RecordDeliveryEventService;
import com.notificationhub.analytics.domain.model.DailyStats;
import com.notificationhub.analytics.domain.model.DeliveryEvent;
import com.notificationhub.analytics.domain.port.in.RecordDeliveryEventUseCase;
import com.notificationhub.analytics.domain.port.out.DailyStatsRepository;
import com.notificationhub.analytics.domain.port.out.DeliveryEventRepository;
import com.notificationhub.analytics.domain.port.out.RealtimeCounterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RecordDeliveryEventServiceTest {

    @Mock DeliveryEventRepository deliveryEventRepository;
    @Mock DailyStatsRepository dailyStatsRepository;
    @Mock RealtimeCounterPort realtimeCounterPort;

    RecordDeliveryEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordDeliveryEventService(deliveryEventRepository, dailyStatsRepository, realtimeCounterPort);
    }

    @Test
    @DisplayName("성공 이벤트 기록 — MongoDB 저장 + Redis 카운터 증가")
    void record_successEvent_savesAndIncrementsCounter() {
        given(deliveryEventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(dailyStatsRepository.findByTenantIdAndDate(anyString(), any()))
                .willReturn(Optional.empty());
        given(dailyStatsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RecordDeliveryEventUseCase.Command cmd = new RecordDeliveryEventUseCase.Command(
                "log-1", "notif-1", "tenant-1", "EMAIL", "SUCCESS", null,
                LocalDateTime.of(2026, 3, 17, 10, 0)
        );
        useCase.record(cmd);

        then(deliveryEventRepository).should().save(any(DeliveryEvent.class));
        then(realtimeCounterPort).should().incrementSuccess("tenant-1", "EMAIL");
        then(dailyStatsRepository).should().save(any(DailyStats.class));
    }

    @Test
    @DisplayName("실패 이벤트 기록 — Redis 실패 카운터 증가")
    void record_failureEvent_incrementsFailureCounter() {
        given(deliveryEventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(dailyStatsRepository.findByTenantIdAndDate(anyString(), any()))
                .willReturn(Optional.empty());
        given(dailyStatsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RecordDeliveryEventUseCase.Command cmd = new RecordDeliveryEventUseCase.Command(
                "log-2", "notif-2", "tenant-1", "SMS", "FAILED", "Timeout",
                LocalDateTime.of(2026, 3, 17, 10, 0)
        );
        useCase.record(cmd);

        then(realtimeCounterPort).should().incrementFailure("tenant-1", "SMS");
    }

    @Test
    @DisplayName("기존 DailyStats가 있으면 업데이트")
    void record_existingDailyStats_updatesInsteadOfCreate() {
        DailyStats existing = DailyStats.create("tenant-1", LocalDate.of(2026, 3, 17));
        given(deliveryEventRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(dailyStatsRepository.findByTenantIdAndDate("tenant-1", LocalDate.of(2026, 3, 17)))
                .willReturn(Optional.of(existing));
        given(dailyStatsRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RecordDeliveryEventUseCase.Command cmd = new RecordDeliveryEventUseCase.Command(
                "log-3", "notif-3", "tenant-1", "PUSH", "SUCCESS", null,
                LocalDateTime.of(2026, 3, 17, 10, 0)
        );
        useCase.record(cmd);

        then(dailyStatsRepository).should().save(argThat(s -> s.getTotalSuccess() == 1));
    }
}
