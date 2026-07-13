// SMS와 Twilio 설정이 Spring 설정 객체로 바인딩되는지 검증하는 테스트
package com.notificationhub.delivery.infrastructure.sender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TwilioPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("sms.provider와 twilio 설정을 바인딩한다")
    void bind_twilioProperties() {
        contextRunner
                .withPropertyValues(
                        "sms.provider=twilio",
                        "twilio.account-sid=ACtest",
                        "twilio.auth-token=test-token",
                        "twilio.from-number=+15551234567",
                        "twilio.messaging-service-sid=MGtest",
                        "twilio.api-url=https://api.twilio.com/2010-04-01"
                )
                .run(context -> {
                    SmsDeliveryProperties smsProperties = context.getBean(SmsDeliveryProperties.class);
                    TwilioProperties twilioProperties = context.getBean(TwilioProperties.class);

                    assertThat(smsProperties.provider()).isEqualTo("twilio");
                    assertThat(twilioProperties.accountSid()).isEqualTo("ACtest");
                    assertThat(twilioProperties.authToken()).isEqualTo("test-token");
                    assertThat(twilioProperties.fromNumber()).isEqualTo("+15551234567");
                    assertThat(twilioProperties.messagingServiceSid()).isEqualTo("MGtest");
                    assertThat(twilioProperties.apiUrl()).isEqualTo("https://api.twilio.com/2010-04-01");
                });
    }

    @Test
    @DisplayName("SMS provider 미설정 시 logging을 기본값으로 사용한다")
    void bind_defaultProvider() {
        contextRunner.run(context -> {
            SmsDeliveryProperties smsProperties = context.getBean(SmsDeliveryProperties.class);
            TwilioProperties twilioProperties = context.getBean(TwilioProperties.class);

            assertThat(smsProperties.provider()).isEqualTo("logging");
            assertThat(twilioProperties.accountSid()).isEmpty();
            assertThat(twilioProperties.authToken()).isEmpty();
            assertThat(twilioProperties.fromNumber()).isEmpty();
            assertThat(twilioProperties.messagingServiceSid()).isEmpty();
            assertThat(twilioProperties.apiUrl()).isEqualTo("https://api.twilio.com/2010-04-01");
        });
    }

    @EnableConfigurationProperties({SmsDeliveryProperties.class, TwilioProperties.class})
    private static class TestConfig {
    }
}
