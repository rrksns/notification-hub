package com.notificationhub.user.infrastructure.persistence.repository;

import com.notificationhub.user.infrastructure.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, String> {
    Optional<TenantEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
