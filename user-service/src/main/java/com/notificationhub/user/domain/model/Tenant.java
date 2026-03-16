package com.notificationhub.user.domain.model;

import com.notificationhub.user.domain.exception.InvalidTenantException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Tenant {
    private final String id;
    private final String name;
    private final String email;
    private final SubscriptionPlan plan;
    private boolean active;
    private final LocalDateTime createdAt;

    private Tenant(String id, String name, String email, SubscriptionPlan plan, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.plan = plan;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static Tenant create(String name, String email, SubscriptionPlan plan) {
        if (name == null || name.isBlank()) throw new InvalidTenantException("Tenant name is required");
        if (email == null || email.isBlank()) throw new InvalidTenantException("Tenant email is required");
        return new Tenant(UUID.randomUUID().toString(), name, email, plan, true, LocalDateTime.now());
    }

    public static Tenant reconstruct(String id, String name, String email, SubscriptionPlan plan, boolean active, LocalDateTime createdAt) {
        return new Tenant(id, name, email, plan, active, createdAt);
    }

    public void deactivate() { this.active = false; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public SubscriptionPlan getPlan() { return plan; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
