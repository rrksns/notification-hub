package com.notificationhub.analytics.domain.model;

public record ChannelStats(long successCount, long failureCount) {
    public long total() {
        return successCount + failureCount;
    }
}
