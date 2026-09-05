-- 로그인 이메일 조회를 위한 인덱스를 추가하는 Flyway migration
CREATE INDEX idx_users_email ON users (email);
