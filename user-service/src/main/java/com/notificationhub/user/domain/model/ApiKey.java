package com.notificationhub.user.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ApiKey {
    private final String id;
    private final String tenantId;
    private final String name;
    private final String keyValue;
    private final LocalDateTime expiresAt;
    private boolean revoked;
    private final LocalDateTime createdAt;

    private ApiKey(String id, String tenantId, String name, String keyValue, LocalDateTime expiresAt, boolean revoked, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.keyValue = keyValue;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public static ApiKey create(String tenantId, String name) {
        return new ApiKey(
            UUID.randomUUID().toString(), tenantId, name,
            "nhub_" + UUID.randomUUID().toString().replace("-", ""),
            null, false, LocalDateTime.now()
        );
    }

    public static ApiKey createWithExpiry(String tenantId, String name, LocalDateTime expiresAt) {
        return new ApiKey(
            UUID.randomUUID().toString(), tenantId, name,
            "nhub_" + UUID.randomUUID().toString().replace("-", ""),
            expiresAt, false, LocalDateTime.now()
        );
    }

    public static ApiKey reconstruct(String id, String tenantId, String name, String keyValue, LocalDateTime expiresAt, boolean revoked, LocalDateTime createdAt) {
        return new ApiKey(id, tenantId, name, keyValue, expiresAt, revoked, createdAt);
    }

    public void revoke() { this.revoked = true; }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getKeyValue() { return keyValue; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
