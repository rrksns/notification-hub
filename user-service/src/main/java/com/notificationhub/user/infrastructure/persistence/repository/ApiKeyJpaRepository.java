package com.notificationhub.user.infrastructure.persistence.repository;

import com.notificationhub.user.infrastructure.persistence.entity.ApiKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKeyEntity, String> {
    Optional<ApiKeyEntity> findByKeyValue(String keyValue);
    List<ApiKeyEntity> findByTenantId(String tenantId);
}
