package com.notificationhub.user.domain.port.out;

import com.notificationhub.user.domain.model.ApiKey;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository {
    ApiKey save(ApiKey apiKey);
    Optional<ApiKey> findByKeyValue(String keyValue);
    List<ApiKey> findByTenantId(String tenantId);
}
