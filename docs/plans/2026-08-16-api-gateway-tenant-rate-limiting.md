# API Gateway Tenant Rate Limiting Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** API Gateway Rate Limiting key를 IP 단일 기준에서 테넌트 우선 기준으로 바꾼다.

**Architecture:** 기존 Spring Cloud Gateway `RequestRateLimiter`와 Redis token bucket 설정은 유지한다. `GatewayConfig`에 `tenantRateLimitKeyResolver`를 두고 `X-Tenant-Id`, `X-Forwarded-For`, remote address 순서로 key를 만든다. 보호된 라우트에서 `X-Tenant-Id`는 `JwtAuthenticationFilter`가 JWT 기준으로 재주입한 값이다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Cloud Gateway, Reactor, JUnit 5.

---

### Task 1: 문서와 진행 기록 준비

**Files:**
- Create: `docs/plans/2026-08-16-api-gateway-tenant-rate-limiting-design.md`
- Create: `docs/plans/2026-08-16-api-gateway-tenant-rate-limiting.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: 설계와 구현 계획을 작성한다**

테넌트 우선 key resolver, X-Forwarded-For fallback, 제외 범위를 문서화한다.

**Step 2: checklist와 context notes를 갱신한다**

이번 작업의 TDD, 구현, 검증, 커밋 항목을 추가한다.

### Task 2: RED 테스트 작성

**Files:**
- Create: `api-gateway/src/test/java/com/notificationhub/gateway/config/GatewayConfigTest.java`

**Step 1: Write the failing test**

`GatewayConfig.tenantRateLimitKeyResolver()`가 다음 key를 반환해야 한다고 검증한다.

- `X-Tenant-Id`가 있으면 `tenant:{tenantId}`
- `X-Forwarded-For`가 있으면 첫 IP로 `ip:{ip}`
- 둘 다 없으면 remote address로 `ip:{ip}`

**Step 2: Run test to verify it fails**

Run: `mvn test -pl api-gateway -am -Dtest=GatewayConfigTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: `tenantRateLimitKeyResolver`가 아직 없어서 컴파일 실패한다.

### Task 3: 최소 구현

**Files:**
- Modify: `api-gateway/src/main/java/com/notificationhub/gateway/config/GatewayConfig.java`
- Modify: `api-gateway/src/main/resources/application.yml`

**Step 1: Implement minimal code**

`tenantRateLimitKeyResolver` bean을 추가하고 기존 IP resolver를 대체한다.

**Step 2: Update route config**

`key-resolver: "#{@tenantRateLimitKeyResolver}"`로 변경한다.

**Step 3: Run focused tests**

Run: `mvn test -pl api-gateway -am -Dtest=GatewayConfigTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS.

### Task 4: 문서 상태 갱신과 검증

**Files:**
- Modify: `api-gateway/API-GATEWAY-FLOW.md`
- Modify: `docs/kafka-redis.md`
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: Update docs**

Rate Limit key 기준을 테넌트 우선, XFF fallback으로 설명하고 P2 #19 상태를 완료로 바꾼다.

**Step 2: Run verification**

Run: `mvn test -pl api-gateway -am`

Expected: PASS.

**Step 3: Manual surface check**

Run: focused test output and config search to confirm the route references `tenantRateLimitKeyResolver`.

**Step 4: Commit**

Run: `git add ...`
Run: `git commit -m "feat: API Gateway 테넌트별 Rate Limiting 적용"`
