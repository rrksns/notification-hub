-- delivery 결과 이벤트 outbox와 notification 중복 방지 제약을 생성하는 migration
ALTER TABLE delivery_logs
    ADD CONSTRAINT uk_delivery_logs_notification_id UNIQUE (notification_id);

CREATE TABLE delivery_result_outbox (
    id VARCHAR(255) NOT NULL,
    delivery_log_id VARCHAR(255) NOT NULL,
    notification_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    result_status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    status ENUM('PENDING', 'PUBLISHED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_delivery_result_outbox_delivery_log UNIQUE (delivery_log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_delivery_result_outbox_status_created
    ON delivery_result_outbox (status, created_at);
