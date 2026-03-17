package com.notificationhub.analytics.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.analytics.domain.model.DailyStats;
import com.notificationhub.analytics.domain.port.in.GetDailyStatsUseCase;
import com.notificationhub.analytics.domain.port.out.DailyStatsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class GetDailyStatsService implements GetDailyStatsUseCase {

    private final DailyStatsRepository dailyStatsRepository;

    public GetDailyStatsService(DailyStatsRepository dailyStatsRepository) {
        this.dailyStatsRepository = dailyStatsRepository;
    }

    @Override
    public DailyStats getByTenantAndDate(String tenantId, LocalDate date) {
        return dailyStatsRepository.findByTenantIdAndDate(tenantId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "No stats for " + date));
    }
}
