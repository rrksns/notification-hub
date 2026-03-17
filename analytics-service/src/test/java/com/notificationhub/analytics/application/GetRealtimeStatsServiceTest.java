package com.notificationhub.analytics.application;

import com.notificationhub.analytics.application.service.GetRealtimeStatsService;
import com.notificationhub.analytics.domain.port.in.GetRealtimeStatsUseCase;
import com.notificationhub.analytics.domain.port.out.RealtimeCounterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GetRealtimeStatsServiceTest {

    @Mock
    RealtimeCounterPort realtimeCounterPort;

    GetRealtimeStatsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetRealtimeStatsService(realtimeCounterPort);
    }

    @Test
    @DisplayName("실시간 통계 조회 — totalSent은 success + failed 합산")
    void getByTenant_returnsSumOfSuccessAndFailed() {
        given(realtimeCounterPort.getTotalSuccess("tenant-1")).willReturn(70L);
        given(realtimeCounterPort.getTotalFailed("tenant-1")).willReturn(30L);

        GetRealtimeStatsUseCase.RealtimeStats stats = useCase.getByTenant("tenant-1");

        assertThat(stats.totalSuccess()).isEqualTo(70L);
        assertThat(stats.totalFailed()).isEqualTo(30L);
        assertThat(stats.totalSent()).isEqualTo(100L);
    }

    @Test
    @DisplayName("실패 없는 경우 totalFailed=0, totalSent=totalSuccess")
    void getByTenant_noFailures_totalSentEqualsSuccess() {
        given(realtimeCounterPort.getTotalSuccess("tenant-2")).willReturn(50L);
        given(realtimeCounterPort.getTotalFailed("tenant-2")).willReturn(0L);

        GetRealtimeStatsUseCase.RealtimeStats stats = useCase.getByTenant("tenant-2");

        assertThat(stats.totalFailed()).isZero();
        assertThat(stats.totalSent()).isEqualTo(50L);
    }

    @Test
    @DisplayName("RealtimeStats record 필드 접근")
    void realtimeStats_recordAccessors() {
        GetRealtimeStatsUseCase.RealtimeStats stats = new GetRealtimeStatsUseCase.RealtimeStats(10L, 2L, 12L);
        assertThat(stats.totalSuccess()).isEqualTo(10L);
        assertThat(stats.totalFailed()).isEqualTo(2L);
        assertThat(stats.totalSent()).isEqualTo(12L);
    }
}
