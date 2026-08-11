// API Gateway fallback 응답을 검증하는 테스트
package com.notificationhub.gateway.presentation.controller;

import com.notificationhub.gateway.filter.FallbackResponseFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(FallbackController.class)
@Import(FallbackResponseFilter.class)
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("fallback 요청은 503과 공통 오류 응답을 반환한다")
    void fallback_returnsServiceUnavailableError() {
        webTestClient.get()
                .uri("/fallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Service temporarily unavailable");
    }

    @Test
    @DisplayName("fallback POST 요청도 503과 공통 오류 응답을 반환한다")
    void fallbackPost_returnsServiceUnavailableError() {
        webTestClient.post()
                .uri("/fallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Service temporarily unavailable");
    }
}
