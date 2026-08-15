// API Gateway Rate Limiting key resolver 동작을 검증하는 테스트
package com.notificationhub.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class GatewayConfigTest {

    private final GatewayConfig gatewayConfig = new GatewayConfig();

    @Test
    @DisplayName("테넌트 헤더가 있으면 테넌트 기준 Rate Limit key를 반환한다")
    void tenantRateLimitKeyResolver_withTenantHeader_returnsTenantKey() {
        KeyResolver keyResolver = gatewayConfig.tenantRateLimitKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/notifications")
                        .header("X-Tenant-Id", "tenant-001")
                        .remoteAddress(new InetSocketAddress("203.0.113.10", 12345))
        );

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("tenant:tenant-001");
    }

    @Test
    @DisplayName("테넌트 헤더가 없으면 X-Forwarded-For 첫 IP 기준 Rate Limit key를 반환한다")
    void tenantRateLimitKeyResolver_withoutTenantHeader_returnsForwardedForKey() {
        KeyResolver keyResolver = gatewayConfig.tenantRateLimitKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .header("X-Forwarded-For", "198.51.100.7, 203.0.113.10")
                        .remoteAddress(new InetSocketAddress("203.0.113.10", 12345))
        );

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:198.51.100.7");
    }

    @Test
    @DisplayName("테넌트와 X-Forwarded-For가 없으면 remote address 기준 Rate Limit key를 반환한다")
    void tenantRateLimitKeyResolver_withoutHeaders_returnsRemoteAddressKey() {
        KeyResolver keyResolver = gatewayConfig.tenantRateLimitKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login")
                        .remoteAddress(new InetSocketAddress("192.0.2.15", 12345))
        );

        String key = keyResolver.resolve(exchange).block();

        assertThat(key).isEqualTo("ip:192.0.2.15");
    }
}
