# API Gateway 테넌트별 Rate Limiting 설계

## 목표

API Gateway의 보호된 요청 Rate Limiting 기준을 단순 IP에서 테넌트 우선 기준으로 바꾼다.
JWT 인증을 통과한 요청은 `X-Tenant-Id`를 key로 쓰고, 테넌트 헤더가 아직 없는 요청은 프록시 환경을 고려해 `X-Forwarded-For` 첫 IP를 우선 사용한다.

## 접근

- 기존 Spring Cloud Gateway `RequestRateLimiter`와 Redis token bucket 설정은 유지한다.
- `GatewayConfig`의 key resolver만 `tenantRateLimitKeyResolver`로 교체한다.
- key 우선순위는 `X-Tenant-Id`, `X-Forwarded-For` 첫 값, remote address, `unknown` 순서다.
- 클라이언트가 보낸 `X-Tenant-Id`는 기존 `JwtAuthenticationFilter`가 제거 후 JWT 기준으로 재주입하므로, 보호된 라우트에서는 위조 헤더를 신뢰하지 않는다.

## 제외 범위

- replenishRate, burstCapacity 값은 변경하지 않는다.
- 공개 `/api/auth/**` 경로에는 Rate Limiter를 새로 적용하지 않는다.
- 각 내부 서비스에 JWT 필터나 NetworkPolicy를 추가하는 작업은 P2 #25로 분리한다.

## 검증

- `GatewayConfig` 단위 테스트로 테넌트, `X-Forwarded-For`, remote address fallback key를 검증한다.
- `api-gateway` focused test와 전체 module test를 실행한다.
- 설정 문서에서 Rate Limit 기준이 테넌트 우선으로 설명되는지 확인한다.
