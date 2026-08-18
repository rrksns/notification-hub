# JPA Optimistic Locking Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** JPA 엔티티에 `@Version Long version` 필드를 추가해 낙관적 락을 활성화한다.

**Architecture:** persistence entity에만 version 필드를 추가한다. 도메인 모델, mapper, DTO, API contract는 그대로 유지하고, 구조 테스트로 모든 JPA 엔티티가 version 필드를 갖는지 검증한다.

**Tech Stack:** Java 17, Jakarta Persistence, JUnit 5, AssertJ.

---

### Task 1: RED 구조 테스트 작성

**Files:**
- Create: `user-service/src/test/java/com/notificationhub/user/infrastructure/persistence/entity/JpaOptimisticLockingTest.java`
- Create: `notification-service/src/test/java/com/notificationhub/notification/infrastructure/persistence/entity/JpaOptimisticLockingTest.java`
- Create: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/persistence/entity/JpaOptimisticLockingTest.java`

**Step 1: 실패하는 테스트 추가**

각 테스트는 대상 entity class에 `version` 필드가 있고, 타입이 `Long`이며, `jakarta.persistence.Version` annotation이 붙어야 한다고 검증한다.

**Step 2: RED 확인**

Run: `mvn test -pl user-service,notification-service,delivery-service -am -Dtest=JpaOptimisticLockingTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: 현재 엔티티에 `version` 필드가 없어 테스트 실패.

### Task 2: version 필드 추가

**Files:**
- Modify: `user-service/src/main/java/com/notificationhub/user/infrastructure/persistence/entity/TenantEntity.java`
- Modify: `user-service/src/main/java/com/notificationhub/user/infrastructure/persistence/entity/UserEntity.java`
- Modify: `user-service/src/main/java/com/notificationhub/user/infrastructure/persistence/entity/ApiKeyEntity.java`
- Modify: `notification-service/src/main/java/com/notificationhub/notification/infrastructure/persistence/entity/NotificationEntity.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/persistence/entity/DeliveryLogEntity.java`

**Step 1: 필드 추가**

각 entity의 `@Id` 필드 아래에 `@Version private Long version;`을 추가한다.

**Step 2: GREEN 확인**

Run: `mvn test -pl user-service,notification-service,delivery-service -am -Dtest=JpaOptimisticLockingTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: 구조 테스트 통과.

### Task 3: 문서와 상태 갱신

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: P2 상태 갱신**

P2 #14를 완료로 변경하고 대상 JPA 엔티티가 `@Version` 필드를 갖는다고 기록한다.

**Step 2: 작업 노트 갱신**

도메인/API contract를 변경하지 않은 이유와 검증 명령을 기록한다.

### Task 4: 검증과 커밋

**Step 1: module test 실행**

Run: `mvn test -pl user-service,notification-service,delivery-service -am`

Expected: 대상 JPA 모듈과 필요한 모듈 테스트 통과.

**Step 2: diff check 실행**

Run: `git diff --check`

Expected: output 없음.

**Step 3: 커밋과 push**

Run: `git add ...`

Run: `git commit -m "feat: JPA 엔티티 낙관적 락 추가"`

Run: `git push`
