package com.notificationhub.analytics.application.service;

import com.notificationhub.analytics.domain.port.in.GetRealtimeStatsUseCase;
import com.notificationhub.analytics.domain.port.out.RealtimeCounterPort;
import org.springframework.stereotype.Service;

@Service
public class GetRealtimeStatsService implements GetRealtimeStatsUseCase {

    private final RealtimeCounterPort realtimeCounterPort;

    public GetRealtimeStatsService(RealtimeCounterPort realtimeCounterPort) {
        this.realtimeCounterPort = realtimeCounterPort;
    }

    @Override
    public RealtimeStats getByTenant(String tenantId) {
        long success = realtimeCounterPort.getTotalSuccess(tenantId);
        long failed = realtimeCounterPort.getTotalFailed(tenantId);
        return new RealtimeStats(success, failed, success + failed);
    }
}
