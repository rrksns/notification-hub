package com.notificationhub.user.application;

import com.notificationhub.user.application.service.TenantService;
import com.notificationhub.user.domain.model.SubscriptionPlan;
import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.domain.port.in.RegisterTenantUseCase;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.domain.port.out.UserRepository;
import com.notificationhub.user.domain.port.out.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterTenantUseCaseTest {

    @Mock TenantRepository tenantRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    RegisterTenantUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TenantService(tenantRepository, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("테넌트 등록 성공 — 테넌트 + 관리자 유저 저장")
    void registerTenant_success() {
        given(tenantRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPw");
        given(tenantRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        RegisterTenantUseCase.Command cmd = new RegisterTenantUseCase.Command(
                "TestCorp", "admin@test.com", "password123"
        );
        RegisterTenantUseCase.Result result = useCase.register(cmd);

        assertThat(result.tenantId()).isNotBlank();
        then(tenantRepository).should().save(any(Tenant.class));
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("이미 등록된 이메일이면 예외 발생")
    void registerTenant_duplicateEmail_throws() {
        given(tenantRepository.existsByEmail("admin@test.com")).willReturn(true);

        RegisterTenantUseCase.Command cmd = new RegisterTenantUseCase.Command(
                "TestCorp", "admin@test.com", "password123"
        );

        assertThatThrownBy(() -> useCase.register(cmd))
                .isInstanceOf(com.notificationhub.common.exception.BusinessException.class);
    }
}
