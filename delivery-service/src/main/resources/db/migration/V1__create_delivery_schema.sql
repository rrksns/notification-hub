-- delivery-service의 초기 MySQL 스키마를 생성하는 Flyway migration
CREATE TABLE delivery_logs (
    id VARCHAR(255) NOT NULL,
    version BIGINT,
    notification_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    channel ENUM('EMAIL', 'SMS', 'PUSH') NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL,
    failure_reason VARCHAR(255),
    attempt_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_delivery_notification_id ON delivery_logs (notification_id);
CREATE INDEX idx_delivery_tenant_id ON delivery_logs (tenant_id);
