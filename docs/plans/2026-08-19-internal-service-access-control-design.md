# 내부 서비스 직접 호출 차단 설계

## 목표

api-gateway를 우회한 내부 서비스 직접 호출을 차단한다.
클라이언트가 `user-service`, `notification-service`, `delivery-service`, `analytics-service`를 직접 호출하더라도 보호된 API는 인증 없이 처리되지 않아야 한다.

## 현재 상태

- api-gateway는 notification, delivery, analytics 라우트에 `JwtAuthentication` 필터를 적용한다.
- api-gateway는 JWT에서 `X-Tenant-Id`, `X-User-Id`를 재주입한다.
- user, notification, delivery, analytics 서비스의 `SecurityConfig`는 현재 `anyRequest().permitAll()`이다.
- K8s에는 NetworkPolicy가 없다.
- user-service의 공개 API는 `/api/users/register`, `/api/auth/login`이고, `/api/keys/**`는 보호되어야 한다.

## 접근안

### 안 A. K8s NetworkPolicy만 적용

장점은 구현이 작고 운영 경계에서 직접 호출을 막을 수 있다는 점이다.
단점은 로컬 실행, Docker Compose, 잘못 노출된 Service/Ingress, 애플리케이션 단 테스트에서는 보호가 없다.

### 안 B. 서비스 레벨 JWT 검증만 적용

장점은 실행 환경과 무관하게 보호된 API가 인증을 강제한다는 점이다.
단점은 각 서비스의 공개 경로와 actuator 경로를 정확히 분리해야 하고, gateway의 user-service 라우팅 정책도 함께 정리해야 한다.

### 안 C. NetworkPolicy와 서비스 레벨 JWT 검증 병행

추천안이다.
NetworkPolicy로 네트워크 경계를 막고, 각 서비스에서 JWT를 다시 검증해 방어선을 하나 더 둔다.
단계적으로는 먼저 서비스 레벨 JWT 검증을 구현하고 테스트로 계약을 고정한 뒤, K8s NetworkPolicy를 추가한다.

## 권장 설계

### 애플리케이션 보안

- 공통 모듈에 재사용 가능한 Servlet JWT 인증 필터를 추가한다.
- 필터는 `Authorization: Bearer {token}`을 검증하고 `X-Tenant-Id`, `X-User-Id`를 JWT claim 기준으로 재주입한다.
- 클라이언트가 보낸 tenant/user 헤더는 각 서비스에서도 제거 후 재주입한다.
- actuator health, readiness, liveness는 공개한다.
- `/actuator/prometheus`는 운영 환경에서는 내부 네트워크에서만 접근하도록 NetworkPolicy로 제한한다.

### 서비스별 경로 정책

| 서비스 | 공개 경로 | 보호 경로 |
|---|---|---|
| user-service | `POST /api/users/register`, `POST /api/auth/login`, actuator health | `/api/keys/**` |
| notification-service | actuator health | `/api/notifications/**` |
| delivery-service | actuator health | `/api/deliveries/**` |
| analytics-service | actuator health | `/api/analytics/**` |

### api-gateway 정리

- `/api/users/register`와 `/api/auth/login`은 공개 라우트로 둔다.
- `/api/keys/**`는 `JwtAuthentication` 필터를 적용한다.
- notification, delivery, analytics 라우트는 현재처럼 `JwtAuthentication`을 유지한다.

### K8s NetworkPolicy

- 외부 ingress는 api-gateway로만 들어오게 한다.
- user, notification, delivery, analytics Pod의 ingress는 api-gateway namespace/pod label에서 오는 트래픽만 허용한다.
- Prometheus가 scraping해야 하는 경우 Prometheus Pod label도 별도 허용한다.
- MySQL, MongoDB, Redis, Kafka는 필요한 application Pod label만 허용한다.

## 검증 기준

- 보호된 API를 토큰 없이 직접 서비스 포트로 호출하면 401을 반환한다.
- 보호된 API를 유효한 JWT로 직접 호출하면 처리된다.
- 클라이언트가 `X-Tenant-Id`를 위조해도 JWT claim 값으로 덮어쓴다.
- 공개 API인 회원가입과 로그인은 기존처럼 동작한다.
- api-gateway 경유 알림 발송 플로우가 깨지지 않는다.
- K8s manifest dry-run 또는 kubeconform 검증이 통과한다.
