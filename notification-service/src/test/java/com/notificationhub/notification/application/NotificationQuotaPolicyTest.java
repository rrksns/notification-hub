package com.notificationhub.notification.application;

import com.notificationhub.notification.domain.quota.NotificationQuotaPolicy;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationQuotaPolicyTest {

    @Test
    void returnsPlanLimitAndMonthlyKey() {
        NotificationQuotaPolicy policy = new NotificationQuotaPolicy();

        assertThat(policy.limitFor("FREE")).isEqualTo(100);
        assertThat(policy.limitFor("PREMIUM")).isEqualTo(10_000);
        assertThat(policy.key("tenant-1", "BASIC", YearMonth.of(2026, 8)))
                .isEqualTo("quota:notification:tenant-1:2026-08");
    }
}
