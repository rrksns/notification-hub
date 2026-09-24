// API Gateway 사용자 라우트 보안 정책을 검증하는 테스트
package com.notificationhub.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

class GatewayRouteConfigTest {

    private final List<Map<String, Object>> routes = gatewayRoutes();

    @Test
    @DisplayName("미연결 API key 경로는 게이트웨이에 노출하지 않는다")
    void apiKeysRoute_isNotExposed() {
        assertThat(routes.stream().map(route -> route.get("id")))
                .doesNotContain("user-service-api-keys");
        assertThat(routes.stream().flatMap(route -> pathPredicates(route).stream()))
                .doesNotContain("Path=/api/keys/**");
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

    @Test
    @DisplayName("공개 Ingress는 TLS Secret을 사용하고 HTTP 접근을 HTTPS로 전환한다")
    void ingress_requiresTlsAndHttpsRedirect() {
        Map<String, Object> ingress = ingressManifest();
        Map<String, Object> metadata = child(ingress, "metadata");
        Map<String, Object> annotations = child(metadata, "annotations");
        Map<String, Object> spec = child(ingress, "spec");
        List<Map<String, Object>> tls = maps(spec, "tls");

        assertThat(annotations)
                .containsEntry("nginx.ingress.kubernetes.io/ssl-redirect", "true")
                .containsEntry("nginx.ingress.kubernetes.io/force-ssl-redirect", "true");
        assertThat(tls).singleElement().satisfies(tlsEntry -> {
            assertThat(tlsEntry.get("secretName")).isEqualTo("notification-hub-ingress-tls");
            assertThat(tlsEntry.get("hosts")).asList().containsExactly("notification-hub.local");
        });
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> ingressManifest() {
        YamlMapFactoryBean factory = new YamlMapFactoryBean();
        factory.setResources(new FileSystemResource("../k8s/api-gateway/ingress.yaml"));
        Map<String, Object> config = factory.getObject();

        assertThat(config).isNotNull();
        return config;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Map<String, Object> source, String key) {
        Object value = source.get(key);
        assertThat(value).isInstanceOf(List.class);
        return (List<Map<String, Object>>) value;
    }
}
