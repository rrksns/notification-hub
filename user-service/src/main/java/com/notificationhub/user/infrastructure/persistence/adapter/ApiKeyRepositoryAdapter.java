package com.notificationhub.user.infrastructure.persistence.adapter;

import com.notificationhub.user.domain.model.ApiKey;
import com.notificationhub.user.domain.port.out.ApiKeyRepository;
import com.notificationhub.user.infrastructure.persistence.entity.ApiKeyEntity;
import com.notificationhub.user.infrastructure.persistence.repository.ApiKeyJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final ApiKeyJpaRepository jpaRepository;

    public ApiKeyRepositoryAdapter(ApiKeyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ApiKey save(ApiKey apiKey) {
        return jpaRepository.save(ApiKeyEntity.from(apiKey)).toDomain();
    }

    @Override
    public Optional<ApiKey> findByKeyValue(String keyValue) {
        return jpaRepository.findByKeyValue(keyValue).map(ApiKeyEntity::toDomain);
    }

    @Override
    public List<ApiKey> findByTenantId(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
                .map(ApiKeyEntity::toDomain)
                .toList();
    }
}
