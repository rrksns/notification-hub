// SendGrid 이메일 발송 어댑터의 REST 요청과 실패 처리를 검증하는 테스트
package com.notificationhub.delivery.infrastructure.sender;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;

class SendGridEmailSenderTest {

    @Test
    @DisplayName("SendGrid Mail Send API 형식으로 이메일을 발송한다")
    void send_postsMailSendRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SendGridEmailSender sender = new SendGridEmailSender(properties(), restClientBuilder);

        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer SG.test-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.personalizations[0].to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.from.email").value("no-reply@example.com"))
                .andExpect(jsonPath("$.from.name").value("Notification Hub"))
                .andExpect(jsonPath("$.subject").value("Notification Hub Alert"))
                .andExpect(jsonPath("$.content[0].type").value("text/plain"))
                .andExpect(jsonPath("$.content[0].value").value("Hello"))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        sender.send("user@example.com", "Hello");

        server.verify();
    }

    @Test
    @DisplayName("SendGrid가 202 외 응답을 반환하면 예외를 던진다")
    void send_providerError_throws() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SendGridEmailSender sender = new SendGridEmailSender(properties(), restClientBuilder);

        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sender.send("user@example.com", "Hello"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("SendGrid delivery failed");

        server.verify();
    }

    @Test
    @DisplayName("SendGrid 필수 설정이 비어 있으면 요청 전 예외를 던진다")
    void send_missingApiKey_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SendGridEmailSender sender = new SendGridEmailSender(
                new SendGridProperties("", "", "Notification Hub",
                        "https://api.sendgrid.com/v3/mail/send", "Notification Hub Alert"),
                restClientBuilder
        );

        assertThatThrownBy(() -> sender.send("user@example.com", "Hello"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("SendGrid API key is required");

        server.verify();
    }

    @Test
    @DisplayName("SendGrid 발신자 이메일이 비어 있으면 요청 전 예외를 던진다")
    void send_missingFromEmail_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SendGridEmailSender sender = new SendGridEmailSender(
                new SendGridProperties("SG.test-key", "", "Notification Hub",
                        "https://api.sendgrid.com/v3/mail/send", "Notification Hub Alert"),
                restClientBuilder
        );

        assertThatThrownBy(() -> sender.send("user@example.com", "Hello"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("SendGrid from email is required");

        server.verify();
    }

    @Test
    @DisplayName("SendGrid 네트워크 오류를 EmailDeliveryException으로 감싼다")
    void send_networkError_wrapsEmailDeliveryException() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        SendGridEmailSender sender = new SendGridEmailSender(properties(), restClientBuilder);

        server.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> sender.send("user@example.com", "Hello"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("SendGrid delivery failed")
                .hasCauseInstanceOf(ResourceAccessException.class);

        server.verify();
    }

    private SendGridProperties properties() {
        return new SendGridProperties(
                "SG.test-key",
                "no-reply@example.com",
                "Notification Hub",
                "https://api.sendgrid.com/v3/mail/send",
                "Notification Hub Alert"
        );
    }
}
