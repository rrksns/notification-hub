package com.notificationhub.analytics.domain.port.in;

public interface GetRealtimeStatsUseCase {
    RealtimeStats getByTenant(String tenantId);

    record RealtimeStats(long totalSuccess, long totalFailed, long totalSent) {}
}
