package com.notificationhub.user.infrastructure.persistence.adapter;

import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.infrastructure.persistence.mapper.TenantMapper;
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
        return TenantMapper.toDomain(jpaRepository.save(TenantMapper.toEntity(tenant)));
    }

    @Override
    public Optional<Tenant> findById(String id) {
        return jpaRepository.findById(id).map(TenantMapper::toDomain);
    }

    @Override
    public Optional<Tenant> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(TenantMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
