// API Gateway 사용자 라우트 보안 정책을 검증하는 테스트
package com.notificationhub.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

class GatewayRouteConfigTest {

    private final List<Map<String, Object>> routes = gatewayRoutes();

    @Test
    @DisplayName("API key 라우트는 JWT 인증과 Rate Limit을 모두 적용한다")
    void apiKeysRoute_requiresJwtAuthenticationAndRateLimiter() {
        Map<String, Object> route = routeById("user-service-api-keys");

        assertThat(pathPredicates(route)).containsExactly("Path=/api/keys/**");
        assertThat(filterNames(route)).contains("JwtAuthentication", "RequestRateLimiter");
    }

    @Test
    @DisplayName("회원 등록 라우트는 공개하되 Rate Limit을 적용한다")
    void registerRoute_isPublicAndRateLimited() {
        Map<String, Object> route = routeById("user-service-register");

        assertThat(pathPredicates(route)).containsExactly("Path=/api/users/register");
        assertThat(filterNames(route))
                .contains("RequestRateLimiter")
                .doesNotContain("JwtAuthentication");
    }

    @Test
    @DisplayName("인증 라우트는 공개 라우트로 유지한다")
    void authRoute_isPublic() {
        Map<String, Object> route = routeById("user-service-auth");

        assertThat(pathPredicates(route)).containsExactly("Path=/api/auth/**");
        assertThat(filterNames(route)).doesNotContain("JwtAuthentication");
    }

    @Test
    @DisplayName("사용자 라우트는 users wildcard와 API key를 같은 공개 라우트로 묶지 않는다")
    void userRoutes_doNotExposeUsersWildcardOrApiKeysTogether() {
        List<String> predicates = routes.stream()
                .flatMap(route -> pathPredicates(route).stream())
                .toList();

        assertThat(predicates)
                .doesNotContain("Path=/api/users/**")
                .doesNotContain("Path=/api/users/**, /api/keys/**");
    }

    private Map<String, Object> routeById(String routeId) {
        return routes.stream()
                .filter(route -> routeId.equals(route.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Route not found: " + routeId));
    }

    private List<String> pathPredicates(Map<String, Object> route) {
        return values(route, "predicates").stream()
                .map(String::valueOf)
                .filter(predicate -> predicate.startsWith("Path="))
                .toList();
    }

    private List<String> filterNames(Map<String, Object> route) {
        return values(route, "filters").stream()
                .map(this::filterName)
                .toList();
    }

    private String filterName(Object filter) {
        if (filter instanceof Map<?, ?> filterMap) {
            Object name = filterMap.get("name");
            return String.valueOf(name);
        }
        return String.valueOf(filter);
    }

    @SuppressWarnings("unchecked")
    private List<Object> values(Map<String, Object> route, String key) {
        Object value = route.get(key);
        if (value == null) {
            return List.of();
        }
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> gatewayRoutes() {
        Map<String, Object> config = applicationConfig();
        Map<String, Object> spring = child(config, "spring");
        Map<String, Object> cloud = child(spring, "cloud");
        Map<String, Object> gateway = child(cloud, "gateway");
        Object routesValue = gateway.get("routes");

        assertThat(routesValue).isInstanceOf(List.class);
        return (List<Map<String, Object>>) routesValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> child(Map<String, Object> source, String key) {
        Object value = source.get(key);

        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    private Map<String, Object> applicationConfig() {
        YamlMapFactoryBean factory = new YamlMapFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Map<String, Object> config = factory.getObject();

        assertThat(config).isNotNull();
        return config;
    }
}
