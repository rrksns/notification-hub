package com.notificationhub.user.infrastructure.security;

import com.notificationhub.common.jwt.JwtTokenProvider;
import com.notificationhub.user.domain.port.out.TokenProvider;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProviderAdapter implements TokenProvider {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtTokenProviderAdapter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String generateToken(String userId, String tenantId, String role) {
        return jwtTokenProvider.generateAccessToken(userId, tenantId, role);
    }

    @Override
    public boolean isValid(String token) {
        return jwtTokenProvider.isValid(token);
    }

    @Override
    public String getUserId(String token) {
        return jwtTokenProvider.getSubject(token);
    }

    @Override
    public String getTenantId(String token) {
        return jwtTokenProvider.getTenantId(token);
    }
}
