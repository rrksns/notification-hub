package com.notificationhub.user.domain;

import com.notificationhub.user.domain.model.ApiKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class ApiKeyTest {

    @Test
    @DisplayName("ApiKey 생성 성공")
    void createApiKey_success() {
        ApiKey apiKey = ApiKey.create("tenant-1", "My Key");
        assertThat(apiKey.getTenantId()).isEqualTo("tenant-1");
        assertThat(apiKey.getName()).isEqualTo("My Key");
        assertThat(apiKey.getKeyValue()).isNotBlank();
        assertThat(apiKey.isExpired()).isFalse();
    }

    @Test
    @DisplayName("만료된 ApiKey는 expired 반환")
    void expiredApiKey_returnsTrue() {
        ApiKey apiKey = ApiKey.createWithExpiry("tenant-1", "Old Key", LocalDateTime.now().minusDays(1));
        assertThat(apiKey.isExpired()).isTrue();
    }

    @Test
    @DisplayName("만료되지 않은 ApiKey는 expired false 반환")
    void notExpiredApiKey_returnsFalse() {
        ApiKey apiKey = ApiKey.createWithExpiry("tenant-1", "New Key", LocalDateTime.now().plusDays(30));
        assertThat(apiKey.isExpired()).isFalse();
    }

    @Test
    @DisplayName("ApiKey revoke 처리")
    void revokeApiKey_success() {
        ApiKey apiKey = ApiKey.create("tenant-1", "My Key");
        apiKey.revoke();
        assertThat(apiKey.isRevoked()).isTrue();
    }
}
