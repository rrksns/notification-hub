package com.notificationhub.user.domain.port.out;

public interface TokenProvider {
    String generateToken(String userId, String tenantId, String role);
    boolean isValid(String token);
    String getUserId(String token);
    String getTenantId(String token);
}
