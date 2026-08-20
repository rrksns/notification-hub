// 공통 Servlet JWT 헤더 인증 필터 동작을 검증하는 테스트
package com.notificationhub.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.notificationhub.common.jwt.JwtProperties;
import com.notificationhub.common.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtHeaderAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties(testSecret(), 60000, 120000)
    );

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("보호 경로에서 Authorization header가 없으면 401을 반환한다")
    void doFilter_withoutAuthorizationHeader_returnsUnauthorized() throws Exception {
        JwtHeaderAuthenticationFilter filter = filterWithExcludedPaths();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.called()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 JWT는 401을 반환한다")
    void doFilter_withInvalidJwt_returnsUnauthorized() throws Exception {
        JwtHeaderAuthenticationFilter filter = filterWithExcludedPaths();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.called()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 JWT는 테넌트와 사용자 헤더를 JWT claim 기준으로 재주입한다")
    void doFilter_withValidJwt_reinjectsTrustedHeaders() throws Exception {
        JwtHeaderAuthenticationFilter filter = filterWithExcludedPaths();
        String token = jwtTokenProvider.generateAccessToken("user-123", "tenant-123", "ADMIN");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notifications");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.addHeader("X-Tenant-Id", "forged-tenant");
        request.addHeader("X-User-Id", "forged-user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        HttpServletRequest forwardedRequest = chain.forwardedRequest();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(forwardedRequest.getHeader("X-Tenant-Id")).isEqualTo("tenant-123");
        assertThat(forwardedRequest.getHeader("X-User-Id")).isEqualTo("user-123");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("제외 경로는 JWT 검증 없이 필터 체인을 통과한다")
    void doFilter_withExcludedPath_skipsJwtValidation() throws Exception {
        JwtHeaderAuthenticationFilter filter = filterWithExcludedPaths("/api/auth/**", "/actuator/health/**");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.called()).isTrue();
        assertThat(chain.forwardedRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private JwtHeaderAuthenticationFilter filterWithExcludedPaths(String... excludedPaths) {
        return new JwtHeaderAuthenticationFilter(
                jwtTokenProvider,
                new JwtSecurityProperties(List.of(excludedPaths))
        );
    }

    private static String testSecret() {
        return Base64.getEncoder()
                .encodeToString("notificationhub-secret-key-for-jwt-signing".getBytes(StandardCharsets.UTF_8));
    }

    private static final class RecordingFilterChain implements FilterChain {

        private boolean called;
        private ServletRequest forwardedRequest;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            this.called = true;
            this.forwardedRequest = request;
        }

        private boolean called() {
            return called;
        }

        private HttpServletRequest forwardedRequest() {
            return (HttpServletRequest) forwardedRequest;
        }
    }
}
