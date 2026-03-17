package com.notificationhub.user.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tenantId;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String encodedPassword;
    private String role;
    private LocalDateTime createdAt;

    protected UserEntity() {}

    public UserEntity(String id, String tenantId, String email, String encodedPassword, String role, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getEncodedPassword() { return encodedPassword; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
