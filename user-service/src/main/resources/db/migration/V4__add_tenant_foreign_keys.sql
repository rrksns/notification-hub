-- 사용자와 API 키의 테넌트 참조 무결성을 추가하는 Flyway migration
ALTER TABLE users
    ADD CONSTRAINT fk_users_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id);

ALTER TABLE api_keys
    ADD CONSTRAINT fk_api_keys_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id);
