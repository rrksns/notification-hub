// notification-service JPA 엔티티의 낙관적 락 필드를 검증하는 테스트
package com.notificationhub.notification.infrastructure.persistence.entity;

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
    @DisplayName("notification JPA 엔티티는 @Version Long 필드를 가진다")
    void notificationEntity_hasVersionField() {
        assertVersionField(NotificationEntity.class);
    }

    @Test
    @DisplayName("notifications created_at 조회 인덱스를 가진다")
    void notificationEntity_hasCreatedAtIndex() {
        assertThat(Arrays.stream(NotificationEntity.class.getAnnotation(Table.class).indexes())
                .anyMatch(index -> "idx_notification_created_at".equals(index.name()))).isTrue();
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
