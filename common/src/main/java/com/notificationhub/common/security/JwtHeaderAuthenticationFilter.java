// 내부 서비스 요청의 JWT를 검증하고 신뢰 헤더를 재주입하는 Servlet 필터
package com.notificationhub.common.security;

import com.notificationhub.common.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtSecurityProperties jwtSecurityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtHeaderAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            JwtSecurityProperties jwtSecurityProperties
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtSecurityProperties = jwtSecurityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return jwtSecurityProperties.excludedPaths().stream()
                .anyMatch(excludedPath -> pathMatcher.match(excludedPath, requestPath));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = bearerToken(request);
        if (!StringUtils.hasText(token) || !jwtTokenProvider.isValid(token)) {
            reject(response);
            return;
        }

        try {
            String tenantId = jwtTokenProvider.getTenantId(token);
            String userId = jwtTokenProvider.getSubject(token);
            if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(userId)) {
                reject(response);
                return;
            }

            setAuthentication(userId);
            filterChain.doFilter(new TrustedHeaderRequestWrapper(request, tenantId, userId), response);
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            reject(response);
        }
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private void setAuthentication(String userId) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void reject(HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.flushBuffer();
    }

    private static final class TrustedHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final String tenantId;
        private final String userId;

        private TrustedHeaderRequestWrapper(HttpServletRequest request, String tenantId, String userId) {
            super(request);
            this.tenantId = tenantId;
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if (isTenantHeader(name)) {
                return tenantId;
            }
            if (isUserHeader(name)) {
                return userId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isTenantHeader(name)) {
                return Collections.enumeration(List.of(tenantId));
            }
            if (isUserHeader(name)) {
                return Collections.enumeration(List.of(userId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            headerNames.removeIf(this::isTrustedHeader);
            headerNames.add(TENANT_ID_HEADER);
            headerNames.add(USER_ID_HEADER);
            return Collections.enumeration(headerNames);
        }

        private boolean isTrustedHeader(String name) {
            return isTenantHeader(name) || isUserHeader(name);
        }

        private static boolean isTenantHeader(String name) {
            return TENANT_ID_HEADER.equalsIgnoreCase(name);
        }

        private static boolean isUserHeader(String name) {
            return USER_ID_HEADER.equalsIgnoreCase(name);
        }
    }
}
