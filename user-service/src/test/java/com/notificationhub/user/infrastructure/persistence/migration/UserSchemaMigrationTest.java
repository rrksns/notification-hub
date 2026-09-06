// user-service 스키마 migration의 테넌트 참조 무결성을 검증하는 테스트
package com.notificationhub.user.infrastructure.persistence.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UserSchemaMigrationTest {

    @Test
    void tenantForeignKeys_referenceTenantTable() throws IOException {
        String migration = readMigration();

        String normalizedMigration = migration.replaceAll("\\s+", " ");

        assertThat(normalizedMigration)
                .contains("CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)")
                .contains("CONSTRAINT fk_api_keys_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)");
    }

    private String readMigration() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V4__add_tenant_foreign_keys.sql")) {
            assertThat(input).as("tenant FK migration resource").isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
