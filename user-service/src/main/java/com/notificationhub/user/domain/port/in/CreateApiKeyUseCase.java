package com.notificationhub.user.domain.port.in;

import java.time.LocalDateTime;

public interface CreateApiKeyUseCase {
    Result create(Command command);

    record Command(String tenantId, String name, LocalDateTime expiresAt) {}
    record Result(String apiKeyId, String keyValue) {}
}
