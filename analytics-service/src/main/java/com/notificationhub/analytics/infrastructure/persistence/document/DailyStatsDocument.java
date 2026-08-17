package com.notificationhub.analytics.infrastructure.persistence.document;

import com.notificationhub.analytics.domain.model.ChannelStats;
import com.notificationhub.analytics.domain.model.DailyStats;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "daily_stats")
public class DailyStatsDocument {

    @Id
    private String id;
    private String tenantId;
    private LocalDate date;
    private long totalSent;
    private long totalSuccess;
    private long totalFailed;
    private Map<String, long[]> channelCounts = new HashMap<>();

    public static DailyStatsDocument from(DailyStats stats) {
        DailyStatsDocument doc = new DailyStatsDocument();
        doc.id = stats.getId();
        doc.tenantId = stats.getTenantId();
        doc.date = stats.getDate();
        doc.totalSent = stats.getTotalSent();
        doc.totalSuccess = stats.getTotalSuccess();
        doc.totalFailed = stats.getTotalFailed();
        stats.getChannelStats().forEach((channel, channelStats) ->
                doc.channelCounts.put(channel, new long[]{channelStats.successCount(), channelStats.failureCount()}));
        return doc;
    }

    public DailyStats toDomain() {
        Map<String, ChannelStats> domainChannelCounts = new HashMap<>();
        channelCounts.forEach((channel, counts) ->
                domainChannelCounts.put(channel, new ChannelStats(counts[0], counts[1])));
        return DailyStats.reconstruct(id, tenantId, date, totalSent, totalSuccess, totalFailed,
                domainChannelCounts);
    }

    public String getTenantId() { return tenantId; }
    public LocalDate getDate() { return date; }
}
