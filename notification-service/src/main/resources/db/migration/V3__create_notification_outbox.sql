-- 알림 Kafka 이벤트의 재발행을 보장하는 outbox 테이블을 생성하는 Flyway migration
CREATE TABLE notification_outbox (
    notification_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    status ENUM('PENDING', 'PUBLISHED') NOT NULL,
    published_at DATETIME(6),
    PRIMARY KEY (notification_id),
    CONSTRAINT uk_notification_outbox_notification UNIQUE (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_outbox_status_occurred
    ON notification_outbox (status, occurred_at);
