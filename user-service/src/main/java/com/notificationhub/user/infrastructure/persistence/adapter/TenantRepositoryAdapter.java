package com.notificationhub.user.infrastructure.persistence.adapter;

import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.infrastructure.persistence.entity.TenantEntity;
import com.notificationhub.user.infrastructure.persistence.repository.TenantJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository jpaRepository;

    public TenantRepositoryAdapter(TenantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        return jpaRepository.save(TenantEntity.from(tenant)).toDomain();
    }

    @Override
    public Optional<Tenant> findById(String id) {
        return jpaRepository.findById(id).map(TenantEntity::toDomain);
    }

    @Override
    public Optional<Tenant> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(TenantEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
