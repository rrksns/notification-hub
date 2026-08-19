# Internal Service Access Control Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** api-gateway를 우회한 내부 서비스 직접 호출을 차단한다.

**Architecture:** 공통 모듈에 Servlet 기반 JWT 인증 필터를 추가하고 각 내부 서비스 SecurityConfig에 적용한다. api-gateway 라우팅은 공개 user endpoint와 보호 endpoint를 분리한다. K8s NetworkPolicy를 추가해 네트워크 레벨에서도 api-gateway와 필요한 운영 Pod만 내부 서비스를 호출하게 제한한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Security Servlet, Spring Cloud Gateway, Kubernetes NetworkPolicy, JUnit 5.

---

### Task 1: Gateway user route 정책 정리

**Files:**
- Modify: `api-gateway/src/main/resources/application.yml`
- Test: `api-gateway/src/test/java/com/notificationhub/gateway/...`

**Step 1: Write the failing test**

Gateway route 설정 테스트를 추가해 `/api/keys/**` 라우트에는 `JwtAuthentication`이 있고, 공개 라우트는 `/api/users/register`와 `/api/auth/**`만 포함해야 한다고 검증한다.

**Step 2: Run test to verify it fails**

Run: `mvn test -pl api-gateway -am -Dtest=<새 테스트명> -Dsurefire.failIfNoSpecifiedTests=false`

Expected: 현재 `/api/users/**, /api/keys/**`가 같은 라우트이고 `/api/keys/**`에 `JwtAuthentication`이 없어 실패한다.

**Step 3: Update route config**

`/api/users/register`와 `/api/auth/**`는 공개 라우트로 두고, `/api/keys/**`는 `JwtAuthentication`과 `RequestRateLimiter`를 적용한다.

**Step 4: Run focused gateway tests**

Run: `mvn test -pl api-gateway -am`

Expected: PASS.

### Task 2: 공통 Servlet JWT 인증 필터 추가

**Files:**
- Create: `common/src/main/java/com/notificationhub/common/security/JwtHeaderAuthenticationFilter.java`
- Create: `common/src/main/java/com/notificationhub/common/security/JwtSecurityProperties.java`
- Test: `common/src/test/java/com/notificationhub/common/security/JwtHeaderAuthenticationFilterTest.java`

**Step 1: Write RED filter tests**

다음을 검증한다.
- Authorization header가 없으면 보호 경로는 401이다.
- 유효하지 않은 JWT는 401이다.
- 유효한 JWT는 `X-Tenant-Id`, `X-User-Id`를 JWT 기준으로 재주입한다.
- 제외 경로는 필터를 통과한다.

**Step 2: Run RED test**

Run: `mvn test -pl common -Dtest=JwtHeaderAuthenticationFilterTest`

Expected: 필터가 없어 컴파일 실패한다.

**Step 3: Implement minimal filter**

`OncePerRequestFilter`로 구현한다.
`JwtTokenProvider`를 사용하고, 제외 경로는 생성자 또는 properties로 받는다.

**Step 4: Run common tests**

Run: `mvn test -pl common`

Expected: PASS.

### Task 3: 내부 서비스 SecurityConfig 적용

**Files:**
- Modify: `user-service/src/main/java/com/notificationhub/user/infrastructure/security/SecurityConfig.java`
- Modify: `notification-service/src/main/java/com/notificationhub/notification/infrastructure/security/SecurityConfig.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/security/SecurityConfig.java`
- Modify: `analytics-service/src/main/java/com/notificationhub/analytics/infrastructure/security/SecurityConfig.java`
- Tests: service별 security slice test 또는 controller test

**Step 1: Write RED service security tests**

각 서비스에서 보호 API를 토큰 없이 호출하면 401이어야 한다고 검증한다.
user-service는 `/api/users/register`, `/api/auth/login`, actuator health가 공개임을 별도 검증한다.

**Step 2: Run RED tests**

Run: service별 focused test command.

Expected: 현재 `permitAll()` 때문에 보호 API가 401이 아니어서 실패한다.

**Step 3: Apply common filter**

서비스별 SecurityConfig에 공통 JWT 필터를 추가한다.
actuator health/readiness/liveness와 user 공개 endpoint만 permit한다.

**Step 4: Run service tests**

Run: `mvn test -pl user-service,notification-service,delivery-service,analytics-service -am`

Expected: PASS.

### Task 4: K8s NetworkPolicy 추가

**Files:**
- Create: `k8s/networkpolicy/api-gateway-to-services.yaml`
- Create: `k8s/networkpolicy/monitoring-to-actuator.yaml`
- Modify: `README.md`
- Modify: `PROCESS.md`

**Step 1: Add policies**

api-gateway Pod에서 내부 서비스 Pod로 가는 ingress만 허용한다.
Prometheus scraping이 필요한 경우 monitoring label을 가진 Pod만 actuator/prometheus 접근을 허용한다.

**Step 2: Validate manifests**

Run: `kubectl apply --dry-run=client -f k8s/networkpolicy/`

Expected: dry-run succeeds.

### Task 5: 최종 검증과 문서 갱신

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`
- Modify: `manual_test.md`

**Step 1: Run full tests**

Run: `mvn test`

Expected: PASS.

**Step 2: Manual QA**

Run local services or targeted security tests to verify.
- direct protected service request without JWT returns 401.
- gateway-routed request with JWT succeeds.
- forged `X-Tenant-Id` is overwritten.

**Step 3: Commit**

Run: `git add ...`
Run: `git commit -m "feat: 내부 서비스 직접 호출 차단"`
