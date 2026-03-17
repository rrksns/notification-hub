package com.notificationhub.user.infrastructure.persistence.entity;

import com.notificationhub.user.domain.model.SubscriptionPlan;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String email;
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;
    private boolean active;
    private LocalDateTime createdAt;

    protected TenantEntity() {}

    public TenantEntity(String id, String name, String email, SubscriptionPlan plan, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.plan = plan;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public SubscriptionPlan getPlan() { return plan; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
