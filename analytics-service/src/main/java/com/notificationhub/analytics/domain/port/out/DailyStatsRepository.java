package com.notificationhub.analytics.domain.port.out;

import com.notificationhub.analytics.domain.model.DailyStats;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyStatsRepository {
    DailyStats save(DailyStats dailyStats);
    Optional<DailyStats> findByTenantIdAndDate(String tenantId, LocalDate date);
}
