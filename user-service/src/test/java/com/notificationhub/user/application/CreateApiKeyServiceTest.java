package com.notificationhub.user.application;

import com.notificationhub.user.application.service.ApiKeyService;
import com.notificationhub.user.domain.model.ApiKey;
import com.notificationhub.user.domain.port.in.CreateApiKeyUseCase;
import com.notificationhub.user.domain.port.out.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CreateApiKeyServiceTest {

    @Mock
    ApiKeyRepository apiKeyRepository;

    CreateApiKeyUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApiKeyService(apiKeyRepository);
    }

    @Test
    @DisplayName("만료일 없이 ApiKey 생성")
    void create_withoutExpiry_savesAndReturnsResult() {
        given(apiKeyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateApiKeyUseCase.Command cmd = new CreateApiKeyUseCase.Command("tenant-1", "My Key", null);
        CreateApiKeyUseCase.Result result = useCase.create(cmd);

        assertThat(result.apiKeyId()).isNotBlank();
        assertThat(result.keyValue()).isNotBlank();
        then(apiKeyRepository).should().save(any(ApiKey.class));
    }

    @Test
    @DisplayName("만료일 있는 ApiKey 생성")
    void create_withExpiry_savesAndReturnsResult() {
        given(apiKeyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        LocalDateTime expiry = LocalDateTime.now().plusDays(30);
        CreateApiKeyUseCase.Command cmd = new CreateApiKeyUseCase.Command("tenant-1", "Expiring Key", expiry);
        CreateApiKeyUseCase.Result result = useCase.create(cmd);

        assertThat(result.apiKeyId()).isNotBlank();
        assertThat(result.keyValue()).isNotBlank();
        then(apiKeyRepository).should().save(argThat(k -> !k.isExpired()));
    }

    @Test
    @DisplayName("Command/Result record 필드 접근")
    void commandAndResult_recordAccessors() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        CreateApiKeyUseCase.Command cmd = new CreateApiKeyUseCase.Command("t-1", "key", expiry);
        assertThat(cmd.tenantId()).isEqualTo("t-1");
        assertThat(cmd.name()).isEqualTo("key");
        assertThat(cmd.expiresAt()).isEqualTo(expiry);

        CreateApiKeyUseCase.Result result = new CreateApiKeyUseCase.Result("id-1", "kv-1");
        assertThat(result.apiKeyId()).isEqualTo("id-1");
        assertThat(result.keyValue()).isEqualTo("kv-1");
    }
}
