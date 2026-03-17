package com.notificationhub.analytics.application;

import com.notificationhub.analytics.application.service.GetDailyStatsService;
import com.notificationhub.analytics.domain.model.DailyStats;
import com.notificationhub.analytics.domain.port.in.GetDailyStatsUseCase;
import com.notificationhub.analytics.domain.port.out.DailyStatsRepository;
import com.notificationhub.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GetDailyStatsServiceTest {

    @Mock
    DailyStatsRepository dailyStatsRepository;

    GetDailyStatsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDailyStatsService(dailyStatsRepository);
    }

    @Test
    @DisplayName("존재하는 날짜 통계 조회 성공")
    void getByTenantAndDate_found_returnsDailyStats() {
        LocalDate date = LocalDate.of(2026, 3, 17);
        DailyStats stats = DailyStats.create("tenant-1", date);
        given(dailyStatsRepository.findByTenantIdAndDate("tenant-1", date)).willReturn(Optional.of(stats));

        DailyStats result = useCase.getByTenantAndDate("tenant-1", date);

        assertThat(result).isEqualTo(stats);
    }

    @Test
    @DisplayName("통계 없을 때 BusinessException 발생")
    void getByTenantAndDate_notFound_throwsBusinessException() {
        LocalDate date = LocalDate.of(2026, 3, 17);
        given(dailyStatsRepository.findByTenantIdAndDate("tenant-1", date)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getByTenantAndDate("tenant-1", date))
                .isInstanceOf(BusinessException.class);
    }
}
