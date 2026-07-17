// FCM PUSH 발송 어댑터의 REST 요청과 실패 처리를 검증하는 테스트
package com.notificationhub.delivery.infrastructure.sender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class FcmPushSenderTest {

    @Test
    @DisplayName("FCM HTTP v1 형식으로 Android PUSH를 발송한다")
    void send_postsFcmMessageRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        FcmPushSender sender = new FcmPushSender(properties(), new FixedAccessTokenProvider("test-access-token"), restClientBuilder);

        server.expect(requestTo("https://fcm.googleapis.com/v1/projects/notification-hub-test/messages:send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-access-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message.token").value("android-fcm-token"))
                .andExpect(jsonPath("$.message.notification.title").value("Notification Hub"))
                .andExpect(jsonPath("$.message.notification.body").value("Hello"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"projects/notification-hub-test/messages/test-message-id\"}"));

        sender.send("android-fcm-token", "Hello");

        server.verify();
    }

    @Test
    @DisplayName("FCM이 2xx 외 응답을 반환하면 예외를 던진다")
    void send_providerError_throws() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        FcmPushSender sender = new FcmPushSender(properties(), new FixedAccessTokenProvider("test-access-token"), restClientBuilder);

        server.expect(requestTo("https://fcm.googleapis.com/v1/projects/notification-hub-test/messages:send"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sender.send("android-fcm-token", "Hello"))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageContaining("FCM delivery failed");

        server.verify();
    }

    @Test
    @DisplayName("FCM project id가 비어 있으면 요청 전 예외를 던진다")
    void send_missingProjectId_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        FcmPushSender sender = new FcmPushSender(
                new FcmProperties("", "", "", "https://fcm.googleapis.com/v1", "Notification Hub"),
                new FixedAccessTokenProvider("test-access-token"),
                restClientBuilder
        );

        assertThatThrownBy(() -> sender.send("android-fcm-token", "Hello"))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageContaining("FCM project id is required");

        server.verify();
    }

    @Test
    @DisplayName("FCM access token이 비어 있으면 요청 전 예외를 던진다")
    void send_missingAccessToken_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        FcmPushSender sender = new FcmPushSender(properties(), new FixedAccessTokenProvider(""), restClientBuilder);

        assertThatThrownBy(() -> sender.send("android-fcm-token", "Hello"))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageContaining("FCM access token is required");

        server.verify();
    }

    @Test
    @DisplayName("FCM 네트워크 오류를 PushDeliveryException으로 감싼다")
    void send_networkError_wrapsPushDeliveryException() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        FcmPushSender sender = new FcmPushSender(properties(), new FixedAccessTokenProvider("test-access-token"), restClientBuilder);

        server.expect(requestTo("https://fcm.googleapis.com/v1/projects/notification-hub-test/messages:send"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> sender.send("android-fcm-token", "Hello"))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessageContaining("FCM delivery failed")
                .hasCauseInstanceOf(ResourceAccessException.class);

        server.verify();
    }

    private FcmProperties properties() {
        return new FcmProperties(
                "notification-hub-test",
                "",
                "",
                "https://fcm.googleapis.com/v1",
                "Notification Hub"
        );
    }

    private static class FixedAccessTokenProvider implements FcmAccessTokenProvider {
        private final String accessToken;

        private FixedAccessTokenProvider(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public String accessToken() {
            return accessToken;
        }
    }
}
