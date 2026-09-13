// 감사 로그를 user-service MySQL에 저장하는 JPA 엔티티
package com.notificationhub.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_tenant_occurred", columnList = "tenantId, occurredAt")
})
public class AuditLogEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tenantId;
    @Column(nullable = false)
    private String actorId;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String resource;
    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected AuditLogEntity() {}

    public AuditLogEntity(String id, String tenantId, String actorId, String action,
                          String resource, LocalDateTime occurredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.action = action;
        this.resource = resource;
        this.occurredAt = occurredAt;
    }

    public String getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getResource() { return resource; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
