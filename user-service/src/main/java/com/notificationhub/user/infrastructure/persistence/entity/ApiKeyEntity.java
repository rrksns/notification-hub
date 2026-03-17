package com.notificationhub.user.infrastructure.persistence.entity;

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

    public ApiKeyEntity(String id, String tenantId, String name, String keyValue,
                        LocalDateTime expiresAt, boolean revoked, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.keyValue = keyValue;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getKeyValue() { return keyValue; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
