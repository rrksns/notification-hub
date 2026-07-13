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
        ChannelDelivererAdapter adapter = new ChannelDelivererAdapter(
                emailSender,
                new RecordingSmsSender(),
                new RecordingPushSender()
        );

        adapter.deliver(ChannelType.EMAIL, "user@example.com", "Hello");

        assertThat(emailSender.recipient).isEqualTo("user@example.com");
        assertThat(emailSender.content).isEqualTo("Hello");
    }

    @Test
    @DisplayName("SMS 채널은 SmsSender로 위임한다")
    void deliver_sms_delegatesToSmsSender() {
        RecordingSmsSender smsSender = new RecordingSmsSender();
        ChannelDelivererAdapter adapter = new ChannelDelivererAdapter(
                new RecordingEmailSender(),
                smsSender,
                new RecordingPushSender()
        );

        adapter.deliver(ChannelType.SMS, "+821012345678", "Hello");

        assertThat(smsSender.recipient).isEqualTo("+821012345678");
        assertThat(smsSender.content).isEqualTo("Hello");
    }

    @Test
    @DisplayName("PUSH 채널은 PushSender로 위임한다")
    void deliver_push_delegatesToPushSender() {
        RecordingPushSender pushSender = new RecordingPushSender();
        ChannelDelivererAdapter adapter = new ChannelDelivererAdapter(
                new RecordingEmailSender(),
                new RecordingSmsSender(),
                pushSender
        );

        adapter.deliver(ChannelType.PUSH, "fcm-token", "Hello");

        assertThat(pushSender.recipient).isEqualTo("fcm-token");
        assertThat(pushSender.content).isEqualTo("Hello");
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

    private static class RecordingSmsSender implements SmsSender {
        private String recipient;
        private String content;

        @Override
        public void send(String recipient, String content) {
            this.recipient = recipient;
            this.content = content;
        }
    }

    private static class RecordingPushSender implements PushSender {
        private String recipient;
        private String content;

        @Override
        public void send(String recipient, String content) {
            this.recipient = recipient;
            this.content = content;
        }
    }
}
