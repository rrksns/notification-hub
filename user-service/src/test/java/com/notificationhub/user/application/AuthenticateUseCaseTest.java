package com.notificationhub.user.application;

import com.notificationhub.user.application.service.AuthService;
import com.notificationhub.user.domain.model.Tenant;
import com.notificationhub.user.domain.model.SubscriptionPlan;
import com.notificationhub.user.domain.model.User;
import com.notificationhub.user.domain.port.in.AuthenticateUseCase;
import com.notificationhub.user.domain.port.out.TenantRepository;
import com.notificationhub.user.domain.port.out.UserRepository;
import com.notificationhub.user.domain.port.out.PasswordEncoder;
import com.notificationhub.user.domain.port.out.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticateUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock TenantRepository tenantRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenProvider tokenProvider;

    AuthenticateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthService(userRepository, tenantRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("유효한 자격증명으로 로그인 성공")
    void authenticate_success() {
        User user = User.create("tenant-1", "user@test.com", "encodedPw");
        Tenant tenant = Tenant.create("Corp", "admin@test.com", SubscriptionPlan.FREE);

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(tenantRepository.findById(user.getTenantId())).willReturn(Optional.of(tenant));
        given(passwordEncoder.matches("rawPw", "encodedPw")).willReturn(true);
        given(tokenProvider.generateToken(anyString(), anyString(), anyString())).willReturn("jwt-token");

        AuthenticateUseCase.Command cmd = new AuthenticateUseCase.Command("user@test.com", "rawPw");
        AuthenticateUseCase.Result result = useCase.authenticate(cmd);

        assertThat(result.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외 발생")
    void authenticate_wrongPassword_throws() {
        User user = User.create("tenant-1", "user@test.com", "encodedPw");

        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPw", "encodedPw")).willReturn(false);

        AuthenticateUseCase.Command cmd = new AuthenticateUseCase.Command("user@test.com", "wrongPw");

        assertThatThrownBy(() -> useCase.authenticate(cmd))
                .isInstanceOf(com.notificationhub.common.exception.BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
    void authenticate_userNotFound_throws() {
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        AuthenticateUseCase.Command cmd = new AuthenticateUseCase.Command("nobody@test.com", "pw");

        assertThatThrownBy(() -> useCase.authenticate(cmd))
                .isInstanceOf(com.notificationhub.common.exception.BusinessException.class);
    }
}
