package com.notificationhub.user.domain.port.in;

public interface RegisterTenantUseCase {
    Result register(Command command);

    record Command(String name, String email, String rawPassword) {}
    record Result(String tenantId, String userId) {}
}
