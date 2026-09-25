// notification-service 다중 replica의 outbox 중복 발행 방지를 검증하는 E2E 테스트
package com.notificationhub.e2e;

import com.notificationhub.common.event.NotificationEvent;
import com.notificationhub.notification.NotificationServiceApplication;
import com.notificationhub.notification.application.service.NotificationOutboxDispatcher;
import com.notificationhub.notification.domain.port.in.CreateNotificationUseCase;
import com.notificationhub.notification.domain.port.out.NotificationEventPublisher;
import com.notificationhub.notification.domain.port.out.NotificationOutboxPort;
import com.notificationhub.notification.domain.port.out.NotificationOutboxMetricsPort;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class NotificationOutboxReplicaIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("notification_service")
            .withUsername("nhub")
            .withPassword("nhub1234");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    private static ConfigurableApplicationContext replicaOne;
    private static ConfigurableApplicationContext replicaTwo;

    @BeforeAll
    static void startReplicas() {
        E2eKafkaSupport.createTopics(kafka.getBootstrapServers(), "notifications");
        replicaOne = startReplica("notification-service-replica-1");
        replicaTwo = startReplica("notification-service-replica-2");
    }

    @AfterAll
    static void stopReplicas() {
        if (replicaTwo != null) {
            replicaTwo.close();
        }
        if (replicaOne != null) {
            replicaOne.close();
        }
    }

    @Test
    @DisplayName("두 notification-service replica가 같은 pending outbox를 한 번만 발행한다")
    void concurrentReplicas_publishPendingOutboxOnce() throws Exception {
        CreateNotificationUseCase createUseCase = replicaOne.getBean(CreateNotificationUseCase.class);
        String tenantId = "tenant-replica-" + UUID.randomUUID();
        String idempotencyKey = "idem-" + UUID.randomUUID();
        CreateNotificationUseCase.Result result = createUseCase.create(new CreateNotificationUseCase.Command(
                tenantId,
                "EMAIL",
                "replica@example.com",
                "Concurrent outbox test",
                idempotencyKey
        ));

        NotificationOutboxDispatcher dispatcherOne = dispatcher(replicaOne);
        NotificationOutboxDispatcher dispatcherTwo = dispatcher(replicaTwo);
        TransactionTemplate transactionOne = transaction(replicaOne);
        TransactionTemplate transactionTwo = transaction(replicaTwo);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> dispatch(dispatcherOne, transactionOne, start));
            var second = executor.submit(() -> dispatch(dispatcherTwo, transactionTwo, start));
            start.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        var events = E2eKafkaSupport.readEvents(
                kafka.getBootstrapServers(),
                "notification-replica-verification-" + UUID.randomUUID(),
                "notifications",
                NotificationEvent.class,
                Duration.ofSeconds(5)
        );
        assertThat(events).hasSize(1);
        assertThat(events.get(0).notificationId()).isEqualTo(result.notificationId());
        assertThat(replicaOne.getBean(NotificationOutboxPort.class).countPending()).isZero();
        assertThat(replicaTwo.getBean(NotificationOutboxPort.class).countPending()).isZero();
    }

    private static NotificationOutboxDispatcher dispatcher(ConfigurableApplicationContext context) {
        return new NotificationOutboxDispatcher(
                context.getBean(NotificationOutboxPort.class),
                context.getBean(NotificationEventPublisher.class),
                context.getBean(NotificationOutboxMetricsPort.class)
        );
    }

    private static TransactionTemplate transaction(ConfigurableApplicationContext context) {
        return new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    private static void dispatch(NotificationOutboxDispatcher dispatcher,
                                 TransactionTemplate transaction,
                                 CountDownLatch start) {
        try {
            start.await(10, TimeUnit.SECONDS);
            transaction.executeWithoutResult(status -> dispatcher.dispatch());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Replica dispatch interrupted", e);
        }
    }

    private static ConfigurableApplicationContext startReplica(String applicationName) {
        return new SpringApplicationBuilder(NotificationServiceApplication.class)
                .web(WebApplicationType.NONE)
                .run(commandLineProperties(Map.ofEntries(
                        Map.entry("spring.main.web-application-type", "none"),
                        Map.entry("spring.application.name", applicationName),
                        Map.entry("eureka.client.enabled", "false"),
                        Map.entry("spring.cloud.discovery.enabled", "false"),
                        Map.entry("jwt.secret", "bm90aWZpY2F0aW9uLW91dGJveC1yZXBsaWNhLXRlc3Qtc2VjcmV0LTMyeA=="),
                        Map.entry("spring.kafka.bootstrap-servers", kafka.getBootstrapServers()),
                        Map.entry("spring.data.redis.host", redis.getHost()),
                        Map.entry("spring.data.redis.port", String.valueOf(redis.getMappedPort(6379))),
                        Map.entry("spring.datasource.url", mysql.getJdbcUrl()),
                        Map.entry("spring.datasource.username", mysql.getUsername()),
                        Map.entry("spring.datasource.password", mysql.getPassword()),
                        Map.entry("spring.flyway.locations", serviceMigrationLocation("notification-service")),
                        Map.entry("notification.outbox-scheduling.enabled", "false"),
                        Map.entry("spring.autoconfigure.exclude", "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
                )));
    }

    private static String[] commandLineProperties(Map<String, String> properties) {
        return properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    private static String serviceMigrationLocation(String serviceName) {
        return "filesystem:" + Path.of("..", serviceName, "src", "main", "resources", "db", "migration")
                .toAbsolutePath()
                .normalize();
    }
}
