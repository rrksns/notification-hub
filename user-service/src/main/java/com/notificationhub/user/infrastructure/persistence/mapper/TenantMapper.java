package com.notificationhub.user.infrastructure.persistence.mapper;

import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.infrastructure.persistence.entity.TenantEntity;

public class TenantMapper {

    private TenantMapper() {}

    public static TenantEntity toEntity(Tenant tenant) {
        return new TenantEntity(
                tenant.getId(),
                tenant.getName(),
                tenant.getEmail(),
                tenant.getPlan(),
                tenant.isActive(),
                tenant.getCreatedAt()
        );
    }

    public static Tenant toDomain(TenantEntity entity) {
        return Tenant.reconstruct(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPlan(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
