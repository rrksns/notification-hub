package com.notificationhub.user.domain;

import com.notificationhub.user.domain.exception.InvalidTenantException;
import com.notificationhub.user.domain.model.SubscriptionPlan;
import com.notificationhub.user.domain.model.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TenantTest {

    @Test
    @DisplayName("유효한 정보로 Tenant 생성 성공")
    void createTenant_success() {
        Tenant tenant = Tenant.create("TestCorp", "admin@test.com", SubscriptionPlan.FREE);
        assertThat(tenant.getName()).isEqualTo("TestCorp");
        assertThat(tenant.getEmail()).isEqualTo("admin@test.com");
        assertThat(tenant.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(tenant.isActive()).isTrue();
        assertThat(tenant.getId()).isNotNull();
    }

    @Test
    @DisplayName("이름이 null이면 예외 발생")
    void createTenant_nullName_throws() {
        assertThatThrownBy(() -> Tenant.create(null, "admin@test.com", SubscriptionPlan.FREE))
                .isInstanceOf(InvalidTenantException.class);
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 예외 발생")
    void createTenant_emptyName_throws() {
        assertThatThrownBy(() -> Tenant.create("", "admin@test.com", SubscriptionPlan.FREE))
                .isInstanceOf(InvalidTenantException.class);
    }

    @Test
    @DisplayName("이메일이 null이면 예외 발생")
    void createTenant_nullEmail_throws() {
        assertThatThrownBy(() -> Tenant.create("TestCorp", null, SubscriptionPlan.FREE))
                .isInstanceOf(InvalidTenantException.class);
    }

    @Test
    @DisplayName("Tenant 비활성화")
    void deactivateTenant_success() {
        Tenant tenant = Tenant.create("TestCorp", "admin@test.com", SubscriptionPlan.FREE);
        tenant.deactivate();
        assertThat(tenant.isActive()).isFalse();
    }
}
