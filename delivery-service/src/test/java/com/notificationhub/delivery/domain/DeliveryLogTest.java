package com.notificationhub.delivery.domain;

import com.notificationhub.delivery.domain.model.ChannelType;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.model.DeliveryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DeliveryLogTest {

    @Test
    @DisplayName("DeliveryLog 생성 시 초기 상태는 PENDING")
    void create_initialStatusIsPending() {
        DeliveryLog log = DeliveryLog.create("notif-1", "tenant-1", ChannelType.EMAIL, "user@example.com");

        assertThat(log.getId()).isNotNull();
        assertThat(log.getNotificationId()).isEqualTo("notif-1");
        assertThat(log.getTenantId()).isEqualTo("tenant-1");
        assertThat(log.getChannel()).isEqualTo(ChannelType.EMAIL);
        assertThat(log.getRecipient()).isEqualTo("user@example.com");
        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(log.getAttemptCount()).isEqualTo(0);
        assertThat(log.getFailureReason()).isNull();
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("PENDING → SUCCESS 상태 전이 성공")
    void markSuccess_transitionToSuccess() {
        DeliveryLog log = DeliveryLog.create("notif-1", "tenant-1", ChannelType.EMAIL, "user@example.com");

        log.markSuccess();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log.getAttemptCount()).isEqualTo(1);
        assertThat(log.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("PENDING → FAILED 상태 전이 성공 (실패 이유 저장)")
    void markFailed_transitionToFailed() {
        DeliveryLog log = DeliveryLog.create("notif-1", "tenant-1", ChannelType.SMS, "+821012345678");

        log.markFailed("SMTP connection timeout");

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(log.getAttemptCount()).isEqualTo(1);
        assertThat(log.getFailureReason()).isEqualTo("SMTP connection timeout");
    }

    @Test
    @DisplayName("이미 SUCCESS 상태에서 markSuccess 호출 시 예외")
    void markSuccess_alreadySuccess_throwsException() {
        DeliveryLog log = DeliveryLog.create("notif-1", "tenant-1", ChannelType.PUSH, "device-token-123");
        log.markSuccess();

        assertThatThrownBy(log::markSuccess)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUCCESS");
    }

    @Test
    @DisplayName("notificationId가 null이면 생성 시 예외")
    void create_nullNotificationId_throwsException() {
        assertThatThrownBy(() -> DeliveryLog.create(null, "tenant-1", ChannelType.EMAIL, "user@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recipient가 blank이면 생성 시 예외")
    void create_blankRecipient_throwsException() {
        assertThatThrownBy(() -> DeliveryLog.create("notif-1", "tenant-1", ChannelType.EMAIL, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ChannelType.from 으로 대소문자 무관하게 변환")
    void channelType_caseInsensitiveParsing() {
        assertThat(ChannelType.from("email")).isEqualTo(ChannelType.EMAIL);
        assertThat(ChannelType.from("SMS")).isEqualTo(ChannelType.SMS);
        assertThat(ChannelType.from("Push")).isEqualTo(ChannelType.PUSH);
    }

    @Test
    @DisplayName("지원하지 않는 채널 타입이면 예외")
    void channelType_unsupported_throwsException() {
        assertThatThrownBy(() -> ChannelType.from("FAX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAX");
    }
}
