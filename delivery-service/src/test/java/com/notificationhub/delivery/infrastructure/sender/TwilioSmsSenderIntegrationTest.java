// Loopback 가짜 Twilio 서버와 Spring SMS 설정의 통합 계약을 검증한다.
package com.notificationhub.delivery.infrastructure.sender;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TwilioSmsSenderIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class, TwilioSmsSender.class);

    @Test
    void sendsTwilioRequestToLoopbackServer() throws Exception {
        try (LocalTwilioServer server = LocalTwilioServer.start()) {
            contextRunner.withPropertyValues(
                    "sms.provider=twilio",
                    "twilio.account-sid=ACtest",
                    "twilio.auth-token=test-token",
                    "twilio.from-number=+15551234567",
                    "twilio.api-url=" + server.apiUrl()
            ).run(context -> {
                assertThat(context).hasSingleBean(TwilioSmsSender.class);
                context.getBean(SmsSender.class).send("+821012345678", "integration test");
            });

            assertThat(server.request().method()).isEqualTo("POST");
            assertThat(server.request().path()).isEqualTo("/2010-04-01/Accounts/ACtest/Messages.json");
            assertThat(server.request().authorization()).isEqualTo("Basic QUN0ZXN0OnRlc3QtdG9rZW4=");
            assertThat(server.request().contentType()).contains("application/x-www-form-urlencoded");
            assertThat(server.request().body()).contains(
                    "To=%2B821012345678",
                    "Body=integration+test",
                    "From=%2B15551234567"
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({SmsDeliveryProperties.class, TwilioProperties.class})
    static class TestConfiguration {
    }

    private static final class LocalTwilioServer implements AutoCloseable {

        private final HttpServer server;
        private final AtomicReference<RecordedRequest> request = new AtomicReference<>();

        private LocalTwilioServer(HttpServer server) {
            this.server = server;
        }

        static LocalTwilioServer start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            LocalTwilioServer fakeTwilio = new LocalTwilioServer(server);
            server.createContext("/2010-04-01/Accounts/ACtest/Messages.json", exchange -> {
                fakeTwilio.request.set(new RecordedRequest(
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Content-Type"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                ));
                exchange.sendResponseHeaders(201, -1);
                exchange.close();
            });
            server.start();
            return fakeTwilio;
        }

        String apiUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/2010-04-01";
        }

        RecordedRequest request() {
            return Objects.requireNonNull(request.get(), "Twilio request was not received");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private record RecordedRequest(String method, String path, String authorization, String contentType, String body) {
    }
}
