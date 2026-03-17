package com.notificationhub.analytics.presentation.dto;

import com.notificationhub.analytics.domain.model.ChannelStats;
import com.notificationhub.analytics.domain.model.DailyStats;

import java.time.LocalDate;
import java.util.Map;

public record DailyStatsResponse(
        String tenantId,
        LocalDate date,
        long totalSent,
        long totalSuccess,
        long totalFailed,
        Map<String, ChannelStats> channelStats
) {
    public static DailyStatsResponse from(DailyStats stats) {
        return new DailyStatsResponse(
                stats.getTenantId(),
                stats.getDate(),
                stats.getTotalSent(),
                stats.getTotalSuccess(),
                stats.getTotalFailed(),
                stats.getChannelStats()
        );
    }
}
