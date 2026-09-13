// notification-service 접수 경로를 실제 MySQL, Redis, Kafka로 검증하는 E2E 테스트
package com.notificationhub.e2e;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.NotificationServiceApplication;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.infrastructure.persistence.entity.NotificationEntity;
import com.notificationhub.notification.infrastructure.persistence.repository.NotificationJpaRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = NotificationServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationAcceptanceIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("notification_service")
            .withUsername("nhub")
            .withPassword("nhub1234");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Autowired
    private CreateNotificationUseCase createNotificationUseCase;

    @Autowired
    private NotificationJpaRepository notificationJpaRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.locations", () -> serviceMigrationLocation("notification-service"));
    }

    @BeforeAll
    static void createKafkaTopics() {
        E2eKafkaSupport.createTopics(kafka.getBootstrapServers(), "notifications");
    }

    @Test
    @DisplayName("알림 접수는 MySQL 저장, Redis 멱등성 키 저장, Kafka 이벤트 발행을 모두 수행한다")
    void createNotification_persistsIdempotencyKeyAndPublishesEvent() {
        String tenantId = "tenant-e2e-" + UUID.randomUUID();
        String idempotencyKey = "idem-" + UUID.randomUUID();

        CreateNotificationUseCase.Command command = new CreateNotificationUseCase.Command(
                tenantId,
                "EMAIL",
                "user@example.com",
                "E2E notification content",
                idempotencyKey
        );

        CreateNotificationUseCase.Result result = createNotificationUseCase.create(command);

        assertThat(result.status()).isEqualTo("PUBLISHED");

        NotificationEntity entity = notificationJpaRepository.findByIdAndTenantId(result.notificationId(), tenantId)
                .orElseThrow();
        assertThat(entity.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(entity.getContent()).isEqualTo("E2E notification content");

        String redisKey = "idempotency:notification:" + tenantId + ":" + idempotencyKey;
        assertThat(redisTemplate.hasKey(redisKey)).isTrue();

        NotificationEvent event = E2eKafkaSupport.readSingleEvent(
                kafka.getBootstrapServers(),
                "notification-acceptance-" + UUID.randomUUID(),
                "notifications",
                NotificationEvent.class,
                Duration.ofSeconds(10)
        );
        assertThat(event.notificationId()).isEqualTo(result.notificationId());
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.idempotencyKey()).isEqualTo(idempotencyKey);
    }

    private static String serviceMigrationLocation(String serviceName) {
        return "filesystem:" + Path.of("..", serviceName, "src", "main", "resources", "db", "migration")
                .toAbsolutePath()
                .normalize();
    }
}
