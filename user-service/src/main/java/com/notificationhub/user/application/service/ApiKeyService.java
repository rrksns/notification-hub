package com.notificationhub.user.application.service;

import com.notificationhub.user.domain.model.ApiKey;
import com.notificationhub.user.domain.model.AuditLog;
import com.notificationhub.user.domain.port.in.CreateApiKeyUseCase;
import com.notificationhub.user.domain.port.out.ApiKeyRepository;
import com.notificationhub.user.domain.port.out.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService implements CreateApiKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditLogRepository auditLogRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AuditLogRepository auditLogRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public Result create(Command command) {
        ApiKey apiKey = command.expiresAt() != null
                ? ApiKey.createWithExpiry(command.tenantId(), command.name(), command.expiresAt())
                : ApiKey.create(command.tenantId(), command.name());

        ApiKey saved = apiKeyRepository.save(apiKey);
        auditLogRepository.save(AuditLog.create(saved.getTenantId(), command.actorId(), "API_KEY_CREATED", saved.getId()));
        return new Result(saved.getId(), saved.getKeyValue());
    }
}
