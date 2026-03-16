package com.notificationhub.user.infrastructure.persistence.entity;

import com.notificationhub.user.domain.model.SubscriptionPlan;
import com.notificationhub.user.domain.model.Tenant;
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

    public static TenantEntity from(Tenant tenant) {
        TenantEntity e = new TenantEntity();
        e.id = tenant.getId();
        e.name = tenant.getName();
        e.email = tenant.getEmail();
        e.plan = tenant.getPlan();
        e.active = tenant.isActive();
        e.createdAt = tenant.getCreatedAt();
        return e;
    }

    public Tenant toDomain() {
        return Tenant.reconstruct(id, name, email, plan, active, createdAt);
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
}
