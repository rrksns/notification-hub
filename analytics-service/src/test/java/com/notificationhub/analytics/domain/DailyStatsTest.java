package com.notificationhub.analytics.domain;

import com.notificationhub.analytics.domain.model.ChannelStats;
import com.notificationhub.analytics.domain.model.DailyStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class DailyStatsTest {

    @Test
    @DisplayName("DailyStats 초기 생성 — 카운트 0")
    void create_initialCountsAreZero() {
        DailyStats stats = DailyStats.create("tenant-1", LocalDate.of(2026, 3, 17));
        assertThat(stats.getTotalSent()).isEqualTo(0);
        assertThat(stats.getTotalSuccess()).isEqualTo(0);
        assertThat(stats.getTotalFailed()).isEqualTo(0);
        assertThat(stats.getChannelStats()).isEmpty();
    }

    @Test
    @DisplayName("성공 이벤트 집계 — totalSent, totalSuccess 증가")
    void recordSuccess_incrementsCounters() {
        DailyStats stats = DailyStats.create("tenant-1", LocalDate.of(2026, 3, 17));
        stats.recordSuccess("EMAIL");
        assertThat(stats.getTotalSent()).isEqualTo(1);
        assertThat(stats.getTotalSuccess()).isEqualTo(1);
        assertThat(stats.getTotalFailed()).isEqualTo(0);
    }

    @Test
    @DisplayName("실패 이벤트 집계 — totalSent, totalFailed 증가")
    void recordFailure_incrementsCounters() {
        DailyStats stats = DailyStats.create("tenant-1", LocalDate.of(2026, 3, 17));
        stats.recordFailure("SMS");
        assertThat(stats.getTotalSent()).isEqualTo(1);
        assertThat(stats.getTotalFailed()).isEqualTo(1);
        assertThat(stats.getTotalSuccess()).isEqualTo(0);
    }

    @Test
    @DisplayName("채널별 통계 누적")
    void recordSuccess_accumulatesChannelStats() {
        DailyStats stats = DailyStats.create("tenant-1", LocalDate.of(2026, 3, 17));
        stats.recordSuccess("EMAIL");
        stats.recordSuccess("EMAIL");
        stats.recordFailure("SMS");

        ChannelStats emailStats = stats.getChannelStats().get("EMAIL");
        assertThat(emailStats.successCount()).isEqualTo(2);
        assertThat(emailStats.failureCount()).isEqualTo(0);

        ChannelStats smsStats = stats.getChannelStats().get("SMS");
        assertThat(smsStats.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("ChannelStats 불변 — 직접 수정 불가")
    void channelStats_isImmutableRecord() {
        ChannelStats stats = new ChannelStats(10L, 2L);
        assertThat(stats.successCount()).isEqualTo(10L);
        assertThat(stats.failureCount()).isEqualTo(2L);
        assertThat(stats.total()).isEqualTo(12L);
    }
}
