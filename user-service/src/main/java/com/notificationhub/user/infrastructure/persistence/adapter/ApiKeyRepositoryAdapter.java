package com.notificationhub.user.infrastructure.persistence.adapter;

import com.notificationhub.user.domain.model.ApiKey;
import com.notificationhub.user.domain.port.out.ApiKeyRepository;
import com.notificationhub.user.infrastructure.persistence.mapper.ApiKeyMapper;
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
        return ApiKeyMapper.toDomain(jpaRepository.save(ApiKeyMapper.toEntity(apiKey)));
    }

    @Override
    public Optional<ApiKey> findByKeyValue(String keyValue) {
        return jpaRepository.findByKeyValue(keyValue).map(ApiKeyMapper::toDomain);
    }

    @Override
    public List<ApiKey> findByTenantId(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
                .map(ApiKeyMapper::toDomain)
                .toList();
    }
}
