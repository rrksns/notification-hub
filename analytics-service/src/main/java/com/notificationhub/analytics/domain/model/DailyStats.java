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
    private final Map<String, long[]> channelCounts; // channel -> [success, failure]

    private DailyStats(String id, String tenantId, LocalDate date,
                       long totalSent, long totalSuccess, long totalFailed,
                       Map<String, long[]> channelCounts) {
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
                                          Map<String, long[]> channelCounts) {
        return new DailyStats(id, tenantId, date, totalSent, totalSuccess, totalFailed, channelCounts);
    }

    public void recordSuccess(String channel) {
        totalSent++;
        totalSuccess++;
        channelCounts.computeIfAbsent(channel, k -> new long[]{0, 0})[0]++;
    }

    public void recordFailure(String channel) {
        totalSent++;
        totalFailed++;
        channelCounts.computeIfAbsent(channel, k -> new long[]{0, 0})[1]++;
    }

    public Map<String, ChannelStats> getChannelStats() {
        Map<String, ChannelStats> result = new HashMap<>();
        channelCounts.forEach((ch, counts) -> result.put(ch, new ChannelStats(counts[0], counts[1])));
        return result;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public LocalDate getDate() { return date; }
    public long getTotalSent() { return totalSent; }
    public long getTotalSuccess() { return totalSuccess; }
    public long getTotalFailed() { return totalFailed; }
    public Map<String, long[]> getRawChannelCounts() { return channelCounts; }
}
