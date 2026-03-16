package com.notificationhub.user.domain;

import com.notificationhub.user.domain.exception.InvalidUserException;
import com.notificationhub.user.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("유효한 정보로 User 생성 성공")
    void createUser_success() {
        User user = User.create("tenant-1", "user@test.com", "encodedPassword123");
        assertThat(user.getTenantId()).isEqualTo("tenant-1");
        assertThat(user.getEmail()).isEqualTo("user@test.com");
        assertThat(user.getId()).isNotNull();
    }

    @Test
    @DisplayName("이메일이 null이면 예외 발생")
    void createUser_nullEmail_throws() {
        assertThatThrownBy(() -> User.create("tenant-1", null, "encodedPassword"))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("패스워드가 null이면 예외 발생")
    void createUser_nullPassword_throws() {
        assertThatThrownBy(() -> User.create("tenant-1", "user@test.com", null))
                .isInstanceOf(InvalidUserException.class);
    }
}
