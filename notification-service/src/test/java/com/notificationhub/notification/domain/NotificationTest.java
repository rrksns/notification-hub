package com.notificationhub.notification.domain;

import com.notificationhub.notification.domain.exception.InvalidNotificationException;
import com.notificationhub.notification.domain.model.Channel;
import com.notificationhub.notification.domain.model.Notification;
import com.notificationhub.notification.domain.model.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NotificationTest {

    @Test
    @DisplayName("유효한 정보로 Notification 생성 성공 — PENDING 상태")
    void createNotification_success() {
        Notification n = Notification.create("tenant-1", Channel.EMAIL, "user@test.com", "Hello", "key-001");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getId()).isNotNull();
        assertThat(n.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("PENDING → PUBLISHED 상태 전이 성공")
    void publish_changesStatusToPublished() {
        Notification n = Notification.create("tenant-1", Channel.EMAIL, "user@test.com", "Hello", "key-001");
        n.publish();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("PUBLISHED 상태에서 다시 publish 시 예외")
    void publish_alreadyPublished_throws() {
        Notification n = Notification.create("tenant-1", Channel.EMAIL, "user@test.com", "Hello", "key-001");
        n.publish();
        assertThatThrownBy(n::publish).isInstanceOf(InvalidNotificationException.class);
    }

    @Test
    @DisplayName("수신자가 null이면 예외 발생")
    void createNotification_nullRecipient_throws() {
        assertThatThrownBy(() -> Notification.create("tenant-1", Channel.EMAIL, null, "Hello", "key-001"))
                .isInstanceOf(InvalidNotificationException.class);
    }

    @Test
    @DisplayName("내용이 빈 문자열이면 예외 발생")
    void createNotification_emptyContent_throws() {
        assertThatThrownBy(() -> Notification.create("tenant-1", Channel.EMAIL, "user@test.com", "", "key-001"))
                .isInstanceOf(InvalidNotificationException.class);
    }
}
