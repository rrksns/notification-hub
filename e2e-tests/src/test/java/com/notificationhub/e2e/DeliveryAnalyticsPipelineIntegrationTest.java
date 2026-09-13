// delivery-service와 analytics-service의 Kafka 파이프라인을 실제 인프라로 검증하는 E2E 테스트
package com.notificationhub.e2e;

import com.notificationhub.analytics.AnalyticsServiceApplication;
import com.notificationhub.analytics.domain.port.out.DeliveryEventRepository;
import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.delivery.DeliveryServiceApplication;
import com.notificationhub.delivery.domain.model.DeliveryStatus;
import com.notificationhub.delivery.infrastructure.persistence.repository.DeliveryLogJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
class DeliveryAnalyticsPipelineIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("delivery_service")
            .withUsername("nhub")
            .withPassword("nhub1234");

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private static ConfigurableApplicationContext deliveryContext;
    private static ConfigurableApplicationContext analyticsContext;

    @BeforeAll
    static void startServices() {
        E2eKafkaSupport.createTopics(kafka.getBootstrapServers(), "notifications", "notifications.dlq",
                "delivery-results", "delivery-results.dlq");

        deliveryContext = startApplication(DeliveryServiceApplication.class, commonProperties(), deliveryProperties());
        analyticsContext = startApplication(AnalyticsServiceApplication.class, commonProperties(), analyticsProperties());
    }

    @AfterAll
    static void stopServices() {
        if (analyticsContext != null) {
            analyticsContext.close();
        }
        if (deliveryContext != null) {
            deliveryContext.close();
        }
    }

    @Test
    @DisplayName("delivery-service 성공 처리 결과는 analytics-service MongoDB와 Redis 집계로 이어진다")
    void notificationEvent_isDeliveredAndRecordedByAnalytics() {
        String tenantId = "tenant-pipeline-" + UUID.randomUUID();
        String notificationId = "notification-" + UUID.randomUUID();

        NotificationEvent event = NotificationEvent.of(
                notificationId,
                tenantId,
                "EMAIL",
                "user@example.com",
                "Pipeline E2E content",
                "idem-" + UUID.randomUUID()
        );

        E2eKafkaSupport.publishEvent(kafka.getBootstrapServers(), "notifications", notificationId, event);

        DeliveryLogJpaRepository deliveryLogRepository =
                deliveryContext.getBean(DeliveryLogJpaRepository.class);
        DeliveryEventRepository deliveryEventRepository =
                analyticsContext.getBean(DeliveryEventRepository.class);
        StringRedisTemplate redisTemplate = analyticsContext.getBean(StringRedisTemplate.class);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var logs = deliveryLogRepository.findByNotificationId(notificationId);
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        });

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var events = deliveryEventRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged());
            assertThat(events.getTotalElements()).isEqualTo(1);
            assertThat(events.getContent().get(0).getNotificationId()).isEqualTo(notificationId);
        });

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(redisTemplate.opsForValue().get("realtime:" + tenantId + ":success:total")).isEqualTo("1")
        );
    }

    @SafeVarargs
    private static ConfigurableApplicationContext startApplication(Class<?> applicationClass, Map<String, String>... properties) {
        return new SpringApplicationBuilder(applicationClass)
                .web(WebApplicationType.NONE)
                .run(commandLineProperties(properties));
    }

    @SafeVarargs
    private static String[] commandLineProperties(Map<String, String>... properties) {
        return Arrays.stream(properties)
                .flatMap(propertyMap -> propertyMap.entrySet().stream())
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    private static Map<String, String> commonProperties() {
        return Map.of(
                "spring.main.web-application-type", "none",
                "eureka.client.enabled", "false",
                "spring.cloud.discovery.enabled", "false",
                "spring.kafka.bootstrap-servers", kafka.getBootstrapServers()
        );
    }

    private static Map<String, String> deliveryProperties() {
        return Map.of(
                "spring.application.name", "delivery-service",
                "spring.datasource.url", mysql.getJdbcUrl(),
                "spring.datasource.username", mysql.getUsername(),
                "spring.datasource.password", mysql.getPassword(),
                "spring.flyway.locations", serviceMigrationLocation("delivery-service"),
                "spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration"
        );
    }

    private static Map<String, String> analyticsProperties() {
        return Map.of(
                "spring.application.name", "analytics-service",
                "spring.data.mongodb.uri", mongo.getReplicaSetUrl("analytics"),
                "spring.data.redis.host", redis.getHost(),
                "spring.data.redis.port", String.valueOf(redis.getMappedPort(6379)),
                "spring.flyway.enabled", "false",
                "spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        );
    }

    private static String serviceMigrationLocation(String serviceName) {
        return "filesystem:" + Path.of("..", serviceName, "src", "main", "resources", "db", "migration")
                .toAbsolutePath()
                .normalize();
    }
}
