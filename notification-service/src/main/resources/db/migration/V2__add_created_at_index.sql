-- notification retention cutoff 조회를 위한 인덱스를 추가하는 Flyway migration
CREATE INDEX idx_notification_created_at ON notifications (created_at);
