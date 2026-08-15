package com.notificationhub.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver tenantRateLimitKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            if (hasText(tenantId)) {
                return Mono.just("tenant:" + tenantId.trim());
            }

            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (hasText(forwardedFor)) {
                return Mono.just("ip:" + forwardedFor.split(",", 2)[0].trim());
            }

            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
