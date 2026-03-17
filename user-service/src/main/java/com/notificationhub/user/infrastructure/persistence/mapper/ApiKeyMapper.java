package com.notificationhub.user.infrastructure.persistence.mapper;

import com.notificationhub.user.domain.model.ApiKey;
import com.notificationhub.user.infrastructure.persistence.entity.ApiKeyEntity;

public class ApiKeyMapper {

    private ApiKeyMapper() {}

    public static ApiKeyEntity toEntity(ApiKey apiKey) {
        return new ApiKeyEntity(
                apiKey.getId(),
                apiKey.getTenantId(),
                apiKey.getName(),
                apiKey.getKeyValue(),
                apiKey.getExpiresAt(),
                apiKey.isRevoked(),
                apiKey.getCreatedAt()
        );
    }

    public static ApiKey toDomain(ApiKeyEntity entity) {
        return ApiKey.reconstruct(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getKeyValue(),
                entity.getExpiresAt(),
                entity.isRevoked(),
                entity.getCreatedAt()
        );
    }
}
