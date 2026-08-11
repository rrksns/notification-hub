# API Gateway fallback 설계

## 목표

Spring Cloud Gateway Circuit Breaker가 `forward:/fallback`으로 보낸 요청에 대해 명확한 `503 Service Unavailable` JSON 응답을 반환한다.

## 현재 문제

- `api-gateway/src/main/resources/application.yml`에는 `fallbackUri: forward:/fallback` 설정이 있다.
- 실제 `/fallback` controller가 없어 fallback 요청이 명확한 서비스 장애 응답으로 처리되지 않는다.
- `docs/improvement-todo.md`의 P2 #20도 이 항목을 미착수로 기록한다.
- `common` 모듈의 Spring Security 의존성 때문에 `/fallback` 직접 요청은 기본 인증에 먼저 걸릴 수 있다.

## 접근

- `api-gateway`에 `FallbackController`를 추가한다.
- `/fallback` 전용 `FallbackResponseFilter`를 최상위 순서로 추가해 기본 보안 체인을 변경하지 않고 fallback 응답을 확정한다.
- endpoint는 `GET`, `POST` 등 method와 무관하게 `/fallback`을 처리한다.
- 응답은 HTTP 503과 `ApiResponse.error("Service temporarily unavailable")` 형식으로 통일한다.
- 새 설정이나 외부 의존성은 추가하지 않는다.

## 성공 기준

- `/fallback` 요청이 HTTP 503을 반환한다.
- 응답 JSON은 `success=false`, `message="Service temporarily unavailable"`를 포함한다.
- `mvn test -pl api-gateway -am`과 `git diff --check`가 통과한다.
