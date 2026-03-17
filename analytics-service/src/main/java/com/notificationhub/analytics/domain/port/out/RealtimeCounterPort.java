package com.notificationhub.analytics.domain.port.out;

public interface RealtimeCounterPort {
    void incrementSuccess(String tenantId, String channel);
    void incrementFailure(String tenantId, String channel);
    long getTotalSuccess(String tenantId);
    long getTotalFailed(String tenantId);
}
