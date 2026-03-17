package com.notificationhub.analytics.domain.port.in;

import com.notificationhub.analytics.domain.model.DailyStats;
import java.time.LocalDate;

public interface GetDailyStatsUseCase {
    DailyStats getByTenantAndDate(String tenantId, LocalDate date);
}
