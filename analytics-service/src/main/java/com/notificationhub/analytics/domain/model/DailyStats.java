package com.notificationhub.analytics.domain.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DailyStats {
    private final String id;
    private final String tenantId;
    private final LocalDate date;
    private long totalSent;
    private long totalSuccess;
    private long totalFailed;
    private final Map<String, ChannelStats> channelCounts;

    private DailyStats(String id, String tenantId, LocalDate date,
                       long totalSent, long totalSuccess, long totalFailed,
                       Map<String, ChannelStats> channelCounts) {
        this.id = id;
        this.tenantId = tenantId;
        this.date = date;
        this.totalSent = totalSent;
        this.totalSuccess = totalSuccess;
        this.totalFailed = totalFailed;
        this.channelCounts = channelCounts;
    }

    public static DailyStats create(String tenantId, LocalDate date) {
        return new DailyStats(tenantId + ":" + date, tenantId, date, 0, 0, 0, new HashMap<>());
    }

    public static DailyStats reconstruct(String id, String tenantId, LocalDate date,
                                          long totalSent, long totalSuccess, long totalFailed,
                                          Map<String, ChannelStats> channelCounts) {
        return new DailyStats(id, tenantId, date, totalSent, totalSuccess, totalFailed, channelCounts);
    }

    public void recordSuccess(String channel) {
        totalSent++;
        totalSuccess++;
        ChannelStats current = channelCounts.getOrDefault(channel, new ChannelStats(0, 0));
        channelCounts.put(channel, new ChannelStats(current.successCount() + 1, current.failureCount()));
    }

    public void recordFailure(String channel) {
        totalSent++;
        totalFailed++;
        ChannelStats current = channelCounts.getOrDefault(channel, new ChannelStats(0, 0));
        channelCounts.put(channel, new ChannelStats(current.successCount(), current.failureCount() + 1));
    }

    public Map<String, ChannelStats> getChannelStats() {
        return new HashMap<>(channelCounts);
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public LocalDate getDate() { return date; }
    public long getTotalSent() { return totalSent; }
    public long getTotalSuccess() { return totalSuccess; }
    public long getTotalFailed() { return totalFailed; }
}
