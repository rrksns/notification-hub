package com.notificationhub.user.infrastructure.persistence.entity;

import com.notificationhub.user.domain.model.ApiKey;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
public class ApiKeyEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tenantId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String keyValue;
    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;

    protected ApiKeyEntity() {}

    public static ApiKeyEntity from(ApiKey apiKey) {
        ApiKeyEntity e = new ApiKeyEntity();
        e.id = apiKey.getId();
        e.tenantId = apiKey.getTenantId();
        e.name = apiKey.getName();
        e.keyValue = apiKey.getKeyValue();
        e.expiresAt = apiKey.getExpiresAt();
        e.revoked = apiKey.isRevoked();
        e.createdAt = apiKey.getCreatedAt();
        return e;
    }

    public ApiKey toDomain() {
        return ApiKey.reconstruct(id, tenantId, name, keyValue, expiresAt, revoked, createdAt);
    }

    public String getKeyValue() { return keyValue; }
    public String getTenantId() { return tenantId; }
}
