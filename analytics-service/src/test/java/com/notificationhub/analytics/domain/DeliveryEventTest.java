package com.notificationhub.analytics.domain;

import com.notificationhub.analytics.domain.model.DeliveryEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class DeliveryEventTest {

    @Test
    @DisplayName("DeliveryEvent 생성 — id 자동 할당")
    void create_assignsRandomId() {
        DeliveryEvent event = DeliveryEvent.create("log-1", "notif-1", "tenant-1", "EMAIL", "SUCCESS", null,
                LocalDateTime.of(2026, 3, 17, 10, 0));

        assertThat(event.getId()).isNotBlank();
        assertThat(event.getDeliveryLogId()).isEqualTo("log-1");
        assertThat(event.getNotificationId()).isEqualTo("notif-1");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
        assertThat(event.getChannel()).isEqualTo("EMAIL");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("SUCCESS 상태는 isSuccess() true 반환")
    void isSuccess_whenStatusSuccess_returnsTrue() {
        DeliveryEvent event = DeliveryEvent.create("log-1", "notif-1", "tenant-1", "EMAIL", "SUCCESS", null,
                LocalDateTime.now());
        assertThat(event.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("FAILED 상태는 isSuccess() false 반환")
    void isSuccess_whenStatusFailed_returnsFalse() {
        DeliveryEvent event = DeliveryEvent.create("log-2", "notif-2", "tenant-1", "SMS", "FAILED", "Timeout",
                LocalDateTime.now());
        assertThat(event.isSuccess()).isFalse();
        assertThat(event.getFailureReason()).isEqualTo("Timeout");
    }

    @Test
    @DisplayName("reconstruct — 지정한 id로 복원")
    void reconstruct_restoresWithGivenId() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 17, 9, 0);
        DeliveryEvent event = DeliveryEvent.reconstruct("fixed-id", "log-1", "notif-1",
                "tenant-1", "PUSH", "SUCCESS", null, ts);

        assertThat(event.getId()).isEqualTo("fixed-id");
        assertThat(event.getOccurredAt()).isEqualTo(ts);
    }
}
