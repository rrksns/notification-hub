package com.notificationhub.user.domain.model;

import com.notificationhub.user.domain.exception.InvalidUserException;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private final String id;
    private final String tenantId;
    private final String email;
    private final String encodedPassword;
    private final String role;
    private final LocalDateTime createdAt;

    private User(String id, String tenantId, String email, String encodedPassword, String role, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static User create(String tenantId, String email, String encodedPassword) {
        if (email == null || email.isBlank()) throw new InvalidUserException("User email is required");
        if (encodedPassword == null || encodedPassword.isBlank()) throw new InvalidUserException("Password is required");
        return new User(UUID.randomUUID().toString(), tenantId, email, encodedPassword, "ADMIN", LocalDateTime.now());
    }

    public static User reconstruct(String id, String tenantId, String email, String encodedPassword, String role, LocalDateTime createdAt) {
        return new User(id, tenantId, email, encodedPassword, role, createdAt);
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getEncodedPassword() { return encodedPassword; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
