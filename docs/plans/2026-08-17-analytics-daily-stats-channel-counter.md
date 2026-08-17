# Analytics DailyStats Channel Counter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `DailyStats` 도메인 내부의 `long[]` 채널 카운터를 명명된 `ChannelStats` 값으로 교체한다.

**Architecture:** 도메인 모델은 `Map<String, ChannelStats>`만 사용한다. Mongo document는 기존 저장 구조를 유지해 데이터 마이그레이션 없이 document/domain 변환에서 배열과 `ChannelStats`를 상호 변환한다. API 응답은 기존 `DailyStatsResponse`가 그대로 `ChannelStats`를 노출한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Data MongoDB, JUnit 5.

---

### Task 1: 진행 문서 추가

**Files:**
- Create: `docs/plans/2026-08-17-analytics-daily-stats-channel-counter-design.md`
- Create: `docs/plans/2026-08-17-analytics-daily-stats-channel-counter.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: 설계와 구현 계획을 작성한다**

도메인 내부 타입 변경과 Mongo 저장 구조 유지 결정을 문서화한다.

**Step 2: checklist와 context notes를 갱신한다**

RED, 구현, 검증, 커밋 항목을 추가한다.

### Task 2: RED 테스트 작성

**Files:**
- Modify: `analytics-service/src/test/java/com/notificationhub/analytics/domain/DailyStatsTest.java`

**Step 1: Write the failing test**

`DailyStats.reconstruct()`가 `Map<String, ChannelStats>`를 받아 채널 카운터를 복원해야 한다는 테스트를 추가한다.

**Step 2: Run test to verify it fails**

Run: `mvn test -pl analytics-service -am -Dtest=DailyStatsTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: 현재 `reconstruct()`가 `Map<String, long[]>`를 받으므로 컴파일 실패한다.

### Task 3: 최소 구현

**Files:**
- Modify: `analytics-service/src/main/java/com/notificationhub/analytics/domain/model/DailyStats.java`
- Modify: `analytics-service/src/main/java/com/notificationhub/analytics/infrastructure/persistence/document/DailyStatsDocument.java`

**Step 1: Replace domain map type**

`DailyStats`의 내부 map을 `Map<String, ChannelStats>`로 바꾸고 `recordSuccess()`/`recordFailure()`에서 새 record 값을 저장한다.

**Step 2: Keep document persistence shape**

`DailyStatsDocument.from()`은 `ChannelStats`를 `long[]`로 변환하고, `toDomain()`은 `long[]`를 `ChannelStats`로 변환한다.

**Step 3: Run focused test**

Run: `mvn test -pl analytics-service -am -Dtest=DailyStatsTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS.

### Task 4: 문서 상태 갱신과 검증

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `PROCESS.md`
- Modify: `context-notes.md`
- Modify: `checklist.md`

**Step 1: Update docs**

P2 #18 상태를 완료로 바꾸고 전체 테스트 수가 바뀌면 갱신한다.

**Step 2: Run verification**

Run: `mvn test -pl analytics-service -am`
Run: `mvn test`
Run: `git diff --check`

Expected: PASS.

**Step 3: Commit**

Run: `git add ...`
Run: `git commit -m "refactor: DailyStats 채널 카운터 명시화"`
