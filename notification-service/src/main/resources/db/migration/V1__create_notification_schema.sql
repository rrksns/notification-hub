-- notification-service의 초기 MySQL 스키마를 생성하는 Flyway migration
CREATE TABLE notifications (
    id VARCHAR(255) NOT NULL,
    version BIGINT,
    tenant_id VARCHAR(255) NOT NULL,
    channel ENUM('EMAIL', 'SMS', 'PUSH'),
    recipient VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'PUBLISHED', 'FAILED'),
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_notifications_idempotency_key UNIQUE (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_tenant_id ON notifications (tenant_id);
