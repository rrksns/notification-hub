package com.notificationhub.notification.domain;

import com.notificationhub.notification.domain.model.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ChannelTest {

    @Test
    @DisplayName("유효한 채널 타입 EMAIL 생성")
    void fromString_email_success() {
        assertThat(Channel.from("EMAIL")).isEqualTo(Channel.EMAIL);
    }

    @Test
    @DisplayName("유효한 채널 타입 SMS 생성")
    void fromString_sms_success() {
        assertThat(Channel.from("SMS")).isEqualTo(Channel.SMS);
    }

    @Test
    @DisplayName("유효한 채널 타입 PUSH 생성")
    void fromString_push_success() {
        assertThat(Channel.from("PUSH")).isEqualTo(Channel.PUSH);
    }

    @Test
    @DisplayName("잘못된 채널 타입이면 예외 발생")
    void fromString_invalid_throws() {
        assertThatThrownBy(() -> Channel.from("FAX"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
