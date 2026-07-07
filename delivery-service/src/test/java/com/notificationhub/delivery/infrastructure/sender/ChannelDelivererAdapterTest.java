// 채널 발송 어댑터의 이메일 위임 동작을 검증하는 테스트
package com.notificationhub.delivery.infrastructure.sender;

import com.notificationhub.delivery.domain.model.ChannelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelDelivererAdapterTest {

    @Test
    @DisplayName("EMAIL 채널은 EmailSender로 위임한다")
    void deliver_email_delegatesToEmailSender() {
        RecordingEmailSender emailSender = new RecordingEmailSender();
        ChannelDelivererAdapter adapter = new ChannelDelivererAdapter(emailSender);

        adapter.deliver(ChannelType.EMAIL, "user@example.com", "Hello");

        assertThat(emailSender.recipient).isEqualTo("user@example.com");
        assertThat(emailSender.content).isEqualTo("Hello");
    }

    private static class RecordingEmailSender implements EmailSender {
        private String recipient;
        private String content;

        @Override
        public void send(String recipient, String content) {
            this.recipient = recipient;
            this.content = content;
        }
    }
}
