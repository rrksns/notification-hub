# API Gateway Fallback Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** API Gateway Circuit Breaker fallback URI `/fallback`에 대해 명확한 503 JSON 응답을 제공한다.

**Architecture:** `api-gateway` presentation layer에 reactive `FallbackController`를 추가한다. 기존 `fallbackUri: forward:/fallback` 설정은 유지한다. `common`의 Spring Security 의존성으로 fallback 요청이 기본 인증에 먼저 걸릴 수 있으므로, `/fallback`만 선처리하는 `FallbackResponseFilter`를 함께 추가해 HTTP 503 JSON 응답을 확정한다.

**Tech Stack:** Spring Cloud Gateway, Spring WebFlux, JUnit 5, WebTestClient.

---

### Task 1: RED 테스트 작성

**Files:**
- Create: `api-gateway/src/test/java/com/notificationhub/gateway/presentation/controller/FallbackControllerTest.java`

**Step 1: 실패하는 테스트 추가**

`FallbackController`를 `@WebFluxTest`로 로드하고 `/fallback` 요청이 503과 error response를 반환해야 한다고 검증한다.

**Step 2: RED 확인**

Run: `mvn test -pl api-gateway -am -Dtest=FallbackControllerTest`

Expected: `FallbackController`가 없어서 컴파일 실패한다.

### Task 2: FallbackController 구현

**Files:**
- Create: `api-gateway/src/main/java/com/notificationhub/gateway/presentation/controller/FallbackController.java`
- Create: `api-gateway/src/main/java/com/notificationhub/gateway/filter/FallbackResponseFilter.java`

**Step 1: 최소 구현 추가**

`@RestController`와 `@RequestMapping("/fallback")`를 사용해 `Mono<ResponseEntity<ApiResponse<Void>>>`를 반환한다.
`FallbackResponseFilter`는 `/fallback` 경로만 `503 Service Unavailable` JSON으로 즉시 응답하고 다른 경로는 기존 필터 체인으로 넘긴다.

**Step 2: GREEN 확인**

Run: `mvn test -pl api-gateway -am -Dtest=FallbackControllerTest`

Expected: 테스트가 통과한다.

### Task 3: 문서 갱신

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `api-gateway/API-GATEWAY-FLOW.md`
- Modify: `context-notes.md`
- Modify: `checklist.md`

**Step 1: P2 상태 갱신**

P2 #20을 완료로 변경하고 fallback 응답이 503임을 기록한다.

**Step 2: Gateway flow 문서 갱신**

장애 처리 섹션에 `/fallback`이 503 JSON 응답을 반환한다고 적는다.

### Task 4: 검증

**Files:**
- Read: `api-gateway/pom.xml`

**Step 1: focused test 실행**

Run: `mvn test -pl api-gateway -am -Dtest=FallbackControllerTest`

Expected: 테스트 통과.

**Step 2: module test 실행**

Run: `mvn test -pl api-gateway -am`

Expected: api-gateway와 필요한 모듈 테스트 통과.

**Step 3: diff check 실행**

Run: `git diff --check`

Expected: output 없음.

### Task 5: 커밋과 push

**Files:**
- Commit: `api-gateway/src/main/java/com/notificationhub/gateway/presentation/controller/FallbackController.java`
- Commit: `api-gateway/src/main/java/com/notificationhub/gateway/filter/FallbackResponseFilter.java`
- Commit: `api-gateway/src/test/java/com/notificationhub/gateway/presentation/controller/FallbackControllerTest.java`
- Commit: `docs/improvement-todo.md`
- Commit: `api-gateway/API-GATEWAY-FLOW.md`
- Commit: `context-notes.md`
- Commit: `checklist.md`
- Commit: `docs/plans/2026-08-11-api-gateway-fallback-design.md`
- Commit: `docs/plans/2026-08-11-api-gateway-fallback.md`

**Step 1: 변경사항 stage**

Run: `git add ...`

Expected: fallback 구현, 테스트, 문서만 staged 상태다.

**Step 2: 커밋**

Run: `git commit -m "feat: API Gateway fallback 응답 추가"`

Expected: fallback 구현 커밋이 생성된다.

**Step 3: push**

Run: `git push`

Expected: 원격 `origin/main`에 반영된다.
