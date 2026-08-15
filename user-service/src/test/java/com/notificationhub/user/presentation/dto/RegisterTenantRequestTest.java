// 테넌트 등록 요청 DTO의 Bean Validation 규칙을 검증하는 테스트
package com.notificationhub.user.presentation.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterTenantRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("복잡도 조건을 만족하는 비밀번호는 검증을 통과한다")
    void password_withRequiredComplexity_isValid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "TestCorp",
                "admin@test.com",
                "ValidPass1!"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("대문자가 없는 비밀번호는 검증에 실패한다")
    void password_withoutUppercase_isInvalid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "TestCorp",
                "admin@test.com",
                "validpass1!"
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("소문자가 없는 비밀번호는 검증에 실패한다")
    void password_withoutLowercase_isInvalid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "TestCorp",
                "admin@test.com",
                "VALIDPASS1!"
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("숫자가 없는 비밀번호는 검증에 실패한다")
    void password_withoutNumber_isInvalid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "TestCorp",
                "admin@test.com",
                "ValidPass!"
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("특수문자가 없는 비밀번호는 검증에 실패한다")
    void password_withoutSpecialCharacter_isInvalid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "TestCorp",
                "admin@test.com",
                "ValidPass1"
        );

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
