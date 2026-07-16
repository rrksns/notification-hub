// PUSH와 FCM 설정이 Spring 설정 객체로 바인딩되는지 검증하는 테스트
package com.notificationhub.delivery.infrastructure.sender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FcmPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("push.provider와 fcm 설정을 바인딩한다")
    void bind_fcmProperties() {
        contextRunner
                .withPropertyValues(
                        "push.provider=fcm",
                        "fcm.project-id=notification-hub-test",
                        "fcm.credentials-json={\"type\":\"service_account\"}",
                        "fcm.credentials-path=/tmp/firebase-service-account.json",
                        "fcm.api-url=https://fcm.googleapis.com/v1",
                        "fcm.title=Notification Hub"
                )
                .run(context -> {
                    PushDeliveryProperties pushProperties = context.getBean(PushDeliveryProperties.class);
                    FcmProperties fcmProperties = context.getBean(FcmProperties.class);

                    assertThat(pushProperties.provider()).isEqualTo("fcm");
                    assertThat(fcmProperties.projectId()).isEqualTo("notification-hub-test");
                    assertThat(fcmProperties.credentialsJson()).isEqualTo("{\"type\":\"service_account\"}");
                    assertThat(fcmProperties.credentialsPath()).isEqualTo("/tmp/firebase-service-account.json");
                    assertThat(fcmProperties.apiUrl()).isEqualTo("https://fcm.googleapis.com/v1");
                    assertThat(fcmProperties.title()).isEqualTo("Notification Hub");
                });
    }

    @Test
    @DisplayName("PUSH provider 미설정 시 logging을 기본값으로 사용한다")
    void bind_defaultProvider() {
        contextRunner.run(context -> {
            PushDeliveryProperties pushProperties = context.getBean(PushDeliveryProperties.class);
            FcmProperties fcmProperties = context.getBean(FcmProperties.class);

            assertThat(pushProperties.provider()).isEqualTo("logging");
            assertThat(fcmProperties.projectId()).isEmpty();
            assertThat(fcmProperties.credentialsJson()).isEmpty();
            assertThat(fcmProperties.credentialsPath()).isEmpty();
            assertThat(fcmProperties.apiUrl()).isEqualTo("https://fcm.googleapis.com/v1");
            assertThat(fcmProperties.title()).isEqualTo("Notification Hub");
        });
    }

    @EnableConfigurationProperties({PushDeliveryProperties.class, FcmProperties.class})
    private static class TestConfig {
    }
}
