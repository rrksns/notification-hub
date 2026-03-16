package com.notificationhub.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpireMs,
        long refreshTokenExpireMs
) {
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        if (accessTokenExpireMs <= 0) {
            throw new IllegalArgumentException("Access token expire time must be positive");
        }
    }
}
