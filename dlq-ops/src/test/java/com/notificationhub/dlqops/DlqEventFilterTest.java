// DLQ 이벤트 필터 조건을 검증하는 테스트
package com.notificationhub.dlqops;

import com.notificationhub.common.event.NotificationEvent;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DlqEventFilterTest {

    @Test
    @DisplayName("필터 옵션이 없으면 모든 이벤트가 일치한다")
    void matches_withoutFilters_returnsTrue() {
        DlqEventFilter filter = new DlqEventFilter(null, null, null);

        assertThat(filter.matches(event("tenant-1", "notification-1", "EMAIL"))).isTrue();
    }

    @Test
    @DisplayName("tenantId notificationId channel이 모두 일치하면 true를 반환한다")
    void matches_allFiltersMatch_returnsTrue() {
        DlqEventFilter filter = new DlqEventFilter("tenant-1", "notification-1", "EMAIL");

        assertThat(filter.matches(event("tenant-1", "notification-1", "EMAIL"))).isTrue();
    }

    @Test
    @DisplayName("하나라도 필터와 다르면 false를 반환한다")
    void matches_anyFilterDiffers_returnsFalse() {
        DlqEventFilter filter = new DlqEventFilter("tenant-1", "notification-1", "EMAIL");

        assertThat(filter.matches(event("tenant-2", "notification-1", "EMAIL"))).isFalse();
        assertThat(filter.matches(event("tenant-1", "notification-2", "EMAIL"))).isFalse();
        assertThat(filter.matches(event("tenant-1", "notification-1", "SMS"))).isFalse();
    }

    private NotificationEvent event(String tenantId, String notificationId, String channel) {
        return new NotificationEvent(
                notificationId,
                tenantId,
                channel,
                "user@example.com",
                "content",
                "idempotency-key",
                Instant.parse("2026-08-26T12:00:00Z")
        );
    }
}
