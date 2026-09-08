// notification-service outbox migration의 저장 구조를 검증하는 테스트
package com.notificationhub.notification.infrastructure.persistence.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationOutboxMigrationTest {

    @Test
    void outboxMigration_definesPendingPayloadAndUniqueNotification() throws IOException {
        String migration = readMigration().replaceAll("\\s+", " ");

        assertThat(migration)
                .contains("CREATE TABLE notification_outbox")
                .contains("notification_id VARCHAR(255) NOT NULL")
                .contains("status ENUM('PENDING', 'PUBLISHED')")
                .contains("CONSTRAINT uk_notification_outbox_notification UNIQUE (notification_id)");
    }

    private String readMigration() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V3__create_notification_outbox.sql")) {
            assertThat(input).as("notification outbox migration resource").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
