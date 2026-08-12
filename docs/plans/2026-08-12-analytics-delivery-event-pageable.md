# Analytics Delivery Event Pageable Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `analytics-service`의 tenant별 delivery event 조회 port가 전체 목록 대신 `Pageable` 기반 `Page`를 사용하도록 변경한다.

**Architecture:** public API는 추가하지 않는다. domain port, Mongo repository, adapter의 기존 미사용 조회 메서드만 페이징 시그니처로 바꾸고, adapter 테스트로 `Pageable` 전달과 page metadata 보존을 검증한다.

**Tech Stack:** Java 17, Spring Data MongoDB, JUnit 5, Mockito.

---

### Task 1: RED adapter 테스트 작성

**Files:**
- Create: `analytics-service/src/test/java/com/notificationhub/analytics/infrastructure/persistence/adapter/DeliveryEventRepositoryAdapterTest.java`

**Step 1: 실패하는 테스트 추가**

`DeliveryEventMongoRepository` mock이 `findByTenantId(tenantId, pageable)`을 반환한다고 가정하고, adapter가 `Pageable`을 그대로 전달하며 `Page<DeliveryEvent>` 메타데이터를 보존하는지 검증한다.

**Step 2: RED 확인**

Run: `mvn test -pl analytics-service -am -Dtest=DeliveryEventRepositoryAdapterTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: 현재 repository port와 Mongo repository가 `Pageable` 시그니처를 제공하지 않아 컴파일 실패한다.

### Task 2: 페이징 시그니처 구현

**Files:**
- Modify: `analytics-service/src/main/java/com/notificationhub/analytics/domain/port/out/DeliveryEventRepository.java`
- Modify: `analytics-service/src/main/java/com/notificationhub/analytics/infrastructure/persistence/repository/DeliveryEventMongoRepository.java`
- Modify: `analytics-service/src/main/java/com/notificationhub/analytics/infrastructure/persistence/adapter/DeliveryEventRepositoryAdapter.java`

**Step 1: port 변경**

`List<DeliveryEvent> findByTenantId(String tenantId)`를 `Page<DeliveryEvent> findByTenantId(String tenantId, Pageable pageable)`로 바꾼다.

**Step 2: Mongo repository 변경**

`List<DeliveryEventDocument> findByTenantId(String tenantId)`를 `Page<DeliveryEventDocument> findByTenantId(String tenantId, Pageable pageable)`로 바꾼다.

**Step 3: adapter 변경**

`mongoRepository.findByTenantId(tenantId, pageable).map(DeliveryEventDocument::toDomain)`을 반환한다.

**Step 4: GREEN 확인**

Run: `mvn test -pl analytics-service -am -Dtest=DeliveryEventRepositoryAdapterTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: adapter 테스트가 통과한다.

### Task 3: 문서와 상태 갱신

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: P2 상태 갱신**

P2 #23을 완료로 변경하고 repository tenant 조회가 `Pageable` 기반으로 바뀌었음을 기록한다.

**Step 2: 작업 노트 갱신**

public API를 추가하지 않은 이유와 검증 명령을 기록한다.

### Task 4: 검증과 커밋

**Step 1: module test 실행**

Run: `mvn test -pl analytics-service -am`

Expected: analytics-service와 필요한 모듈 테스트 통과.

**Step 2: diff check 실행**

Run: `git diff --check`

Expected: output 없음.

**Step 3: 커밋과 push**

Run: `git add ...`

Run: `git commit -m "feat: analytics 이벤트 조회 페이징 적용"`

Run: `git push`
