// user-service JPA 엔티티의 낙관적 락 필드를 검증하는 테스트
package com.notificationhub.user.infrastructure.persistence.entity;

import jakarta.persistence.Version;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaOptimisticLockingTest {

    @Test
    @DisplayName("user JPA 엔티티는 @Version Long 필드를 가진다")
    void userEntities_haveVersionField() {
        assertVersionField(TenantEntity.class);
        assertVersionField(UserEntity.class);
        assertVersionField(ApiKeyEntity.class);
    }

    @Test
    @DisplayName("users email 조회 인덱스를 가진다")
    void userEntity_hasEmailIndex() {
        assertThat(Arrays.stream(UserEntity.class.getAnnotation(Table.class).indexes())
                .anyMatch(index -> "idx_users_email".equals(index.name()))).isTrue();
    }

    private void assertVersionField(Class<?> entityClass) {
        Optional<Field> versionField = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> "version".equals(field.getName()))
                .findFirst();

        assertThat(versionField).isPresent();
        assertThat(versionField.get().getType()).isEqualTo(Long.class);
        assertThat(versionField.get().isAnnotationPresent(Version.class)).isTrue();
    }
}
