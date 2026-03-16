package com.notificationhub.user.application.service;

import com.notificationhub.user.domain.model.ApiKey;
import com.notificationhub.user.domain.port.in.CreateApiKeyUseCase;
import com.notificationhub.user.domain.port.out.ApiKeyRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService implements CreateApiKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public Result create(Command command) {
        ApiKey apiKey = command.expiresAt() != null
                ? ApiKey.createWithExpiry(command.tenantId(), command.name(), command.expiresAt())
                : ApiKey.create(command.tenantId(), command.name());

        ApiKey saved = apiKeyRepository.save(apiKey);
        return new Result(saved.getId(), saved.getKeyValue());
    }
}
