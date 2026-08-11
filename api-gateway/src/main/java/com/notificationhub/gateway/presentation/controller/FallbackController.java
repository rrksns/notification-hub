// API Gateway Circuit Breaker fallback 응답을 제공하는 컨트롤러
package com.notificationhub.gateway.presentation.controller;

import com.notificationhub.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<ApiResponse<Void>>> fallback() {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error("Service temporarily unavailable"))
        );
    }
}
