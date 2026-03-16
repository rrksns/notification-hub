package com.notificationhub.user.domain.port.out;

import com.notificationhub.user.domain.model.Tenant;
import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(String id);
    Optional<Tenant> findByEmail(String email);
    boolean existsByEmail(String email);
}
