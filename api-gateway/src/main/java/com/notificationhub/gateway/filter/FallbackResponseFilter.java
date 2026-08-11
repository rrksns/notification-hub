// API Gateway fallback 경로에 서비스 장애 응답을 즉시 쓰는 필터
package com.notificationhub.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FallbackResponseFilter implements WebFilter {

    private static final String FALLBACK_PATH = "/fallback";
    private static final byte[] RESPONSE_BODY = """
            {"success":false,"message":"Service temporarily unavailable"}
            """.trim().getBytes(StandardCharsets.UTF_8);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestPath = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!FALLBACK_PATH.equals(requestPath)) {
            return chain.filter(exchange);
        }

        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(RESPONSE_BODY)));
    }
}
