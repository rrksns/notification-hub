// delivery-service JPA 엔티티의 낙관적 락 필드를 검증하는 테스트
package com.notificationhub.delivery.infrastructure.persistence.entity;

import jakarta.persistence.Version;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaOptimisticLockingTest {

    @Test
    @DisplayName("delivery JPA 엔티티는 @Version Long 필드를 가진다")
    void deliveryEntity_hasVersionField() {
        assertVersionField(DeliveryLogEntity.class);
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
