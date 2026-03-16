package com.notificationhub.user.infrastructure.persistence.entity;

import com.notificationhub.user.domain.model.User;
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

    public static UserEntity from(User user) {
        UserEntity e = new UserEntity();
        e.id = user.getId();
        e.tenantId = user.getTenantId();
        e.email = user.getEmail();
        e.encodedPassword = user.getEncodedPassword();
        e.role = user.getRole();
        e.createdAt = user.getCreatedAt();
        return e;
    }

    public User toDomain() {
        return User.reconstruct(id, tenantId, email, encodedPassword, role, createdAt);
    }

    public String getEmail() { return email; }
}
