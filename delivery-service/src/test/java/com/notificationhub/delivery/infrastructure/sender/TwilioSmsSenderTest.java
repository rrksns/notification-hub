// Twilio SMS 발송 어댑터의 REST 요청과 실패 처리를 검증하는 테스트
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TwilioSmsSenderTest {

    @Test
    @DisplayName("Twilio Messages API 형식으로 SMS를 발송한다")
    void send_postsTwilioMessageRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(propertiesWithFromNumber(), restClientBuilder);

        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/ACtest/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuthHeader()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("To=%2B821012345678")))
                .andExpect(content().string(containsString("Body=Hello")))
                .andExpect(content().string(containsString("From=%2B15551234567")))
                .andRespond(withStatus(HttpStatus.CREATED));

        sender.send("+821012345678", "Hello");

        server.verify();
    }

    @Test
    @DisplayName("Messaging Service SID가 있으면 From 대신 MessagingServiceSid로 발송한다")
    void send_withMessagingServiceSid_omitsFromNumber() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(propertiesWithMessagingService(), restClientBuilder);

        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/ACtest/Messages.json"))
                .andExpect(content().string(containsString("MessagingServiceSid=MGtest")))
                .andExpect(content().string(not(containsString("From="))))
                .andRespond(withStatus(HttpStatus.CREATED));

        sender.send("+821012345678", "Hello");

        server.verify();
    }

    @Test
    @DisplayName("Twilio가 201 외 응답을 반환하면 예외를 던진다")
    void send_providerError_throws() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(propertiesWithFromNumber(), restClientBuilder);

        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/ACtest/Messages.json"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> sender.send("+821012345678", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("Twilio delivery failed");

        server.verify();
    }

    @Test
    @DisplayName("Twilio 계정 SID가 비어 있으면 요청 전 예외를 던진다")
    void send_missingAccountSid_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(
                new TwilioProperties("", "test-token", "+15551234567", "", "https://api.twilio.com/2010-04-01"),
                restClientBuilder
        );

        assertThatThrownBy(() -> sender.send("+821012345678", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("Twilio account SID is required");

        server.verify();
    }

    @Test
    @DisplayName("Twilio 인증 토큰이 비어 있으면 요청 전 예외를 던진다")
    void send_missingAuthToken_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(
                new TwilioProperties("ACtest", "", "+15551234567", "", "https://api.twilio.com/2010-04-01"),
                restClientBuilder
        );

        assertThatThrownBy(() -> sender.send("+821012345678", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("Twilio auth token is required");

        server.verify();
    }

    @Test
    @DisplayName("발신 번호와 Messaging Service SID가 모두 비어 있으면 요청 전 예외를 던진다")
    void send_missingSender_throwsBeforeRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(
                new TwilioProperties("ACtest", "test-token", "", "", "https://api.twilio.com/2010-04-01"),
                restClientBuilder
        );

        assertThatThrownBy(() -> sender.send("+821012345678", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("Twilio sender is required");

        server.verify();
    }

    @Test
    @DisplayName("Twilio 네트워크 오류를 SmsDeliveryException으로 감싼다")
    void send_networkError_wrapsSmsDeliveryException() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        TwilioSmsSender sender = new TwilioSmsSender(propertiesWithFromNumber(), restClientBuilder);

        server.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/ACtest/Messages.json"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> sender.send("+821012345678", "Hello"))
                .isInstanceOf(SmsDeliveryException.class)
                .hasMessageContaining("Twilio delivery failed")
                .hasCauseInstanceOf(ResourceAccessException.class);

        server.verify();
    }

    private TwilioProperties propertiesWithFromNumber() {
        return new TwilioProperties(
                "ACtest",
                "test-token",
                "+15551234567",
                "",
                "https://api.twilio.com/2010-04-01"
        );
    }

    private TwilioProperties propertiesWithMessagingService() {
        return new TwilioProperties(
                "ACtest",
                "test-token",
                "",
                "MGtest",
                "https://api.twilio.com/2010-04-01"
        );
    }

    private String basicAuthHeader() {
        String token = Base64.getEncoder()
                .encodeToString("ACtest:test-token".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
