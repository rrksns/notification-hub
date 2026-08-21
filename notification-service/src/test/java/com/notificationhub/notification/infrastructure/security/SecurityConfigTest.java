// notification-service SecurityConfig의 직접 호출 차단 정책을 검증하는 테스트
package com.notificationhub.notification.infrastructure.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notificationhub.common.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigTest.SecurityProbeController.class)
@Import({SecurityConfig.class, SecurityConfigTest.SecurityProbeController.class})
@TestPropertySource(properties = {
        "jwt.security.excluded-paths=/actuator/health,/actuator/health/**"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("알림 보호 경로는 토큰 없이 401을 반환한다")
    void notifications_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/notifications/security-probe")
                        .header("X-Tenant-Id", "forged-tenant"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("actuator health 경로는 토큰 없이 접근할 수 있다")
    void actuatorHealth_withoutToken_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 JWT는 위조 테넌트 헤더를 JWT claim 값으로 덮어쓴다")
    void notifications_withValidToken_usesTrustedTenantHeader() throws Exception {
        when(jwtTokenProvider.isValid("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getTenantId("valid-token")).thenReturn("tenant-123");
        when(jwtTokenProvider.getSubject("valid-token")).thenReturn("user-123");

        mockMvc.perform(post("/api/notifications/security-probe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .header("X-Tenant-Id", "forged-tenant"))
                .andExpect(status().isOk())
                .andExpect(content().string("tenant-123"));
    }

    @RestController
    static class SecurityProbeController {

        @PostMapping("/api/notifications/security-probe")
        String protectedNotification(@RequestHeader("X-Tenant-Id") String tenantId) {
            return tenantId;
        }

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }
    }
}
