package com.notificationhub.user.domain.port.in;

public interface AuthenticateUseCase {
    Result authenticate(Command command);

    record Command(String email, String rawPassword) {}
    record Result(String accessToken, String tenantId, String userId) {}
}
