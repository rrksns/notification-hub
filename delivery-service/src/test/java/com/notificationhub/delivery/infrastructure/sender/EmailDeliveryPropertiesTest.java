// 이메일 발송 설정이 Spring 설정 객체로 바인딩되는지 검증하는 테스트
package com.notificationhub.delivery.infrastructure.sender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDeliveryPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("email.provider와 sendgrid 설정을 바인딩한다")
    void bind_sendGridProperties() {
        contextRunner
                .withPropertyValues(
                        "email.provider=sendgrid",
                        "sendgrid.api-key=SG.test-key",
                        "sendgrid.from-email=no-reply@example.com",
                        "sendgrid.from-name=Notification Hub",
                        "sendgrid.api-url=https://api.sendgrid.com/v3/mail/send"
                )
                .run(context -> {
                    EmailDeliveryProperties emailProperties = context.getBean(EmailDeliveryProperties.class);
                    SendGridProperties sendGridProperties = context.getBean(SendGridProperties.class);

                    assertThat(emailProperties.provider()).isEqualTo("sendgrid");
                    assertThat(sendGridProperties.apiKey()).isEqualTo("SG.test-key");
                    assertThat(sendGridProperties.fromEmail()).isEqualTo("no-reply@example.com");
                    assertThat(sendGridProperties.fromName()).isEqualTo("Notification Hub");
                    assertThat(sendGridProperties.apiUrl()).isEqualTo("https://api.sendgrid.com/v3/mail/send");
                });
    }

    @Test
    @DisplayName("provider 미설정 시 logging을 기본값으로 사용한다")
    void bind_defaultProvider() {
        contextRunner
                .withPropertyValues(
                        "sendgrid.api-url=https://api.sendgrid.com/v3/mail/send"
                )
                .run(context -> {
                    EmailDeliveryProperties emailProperties = context.getBean(EmailDeliveryProperties.class);

                    assertThat(emailProperties.provider()).isEqualTo("logging");
                });
    }

    @EnableConfigurationProperties({EmailDeliveryProperties.class, SendGridProperties.class})
    private static class TestConfig {
    }
}
