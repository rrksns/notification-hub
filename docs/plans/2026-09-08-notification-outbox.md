# Notification Outbox 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** notification-service가 알림 저장과 Kafka 이벤트 발행을 Transactional Outbox 방식으로 연결한다.

**Architecture:** 애플리케이션 서비스는 Kafka를 직접 호출하지 않고 `NotificationOutboxPort`에 payload를 저장한다. JPA 기반 dispatcher가 pending outbox를 읽고 기존 Kafka publisher를 호출한 후 성공 시 상태를 갱신한다.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Flyway, Kafka, JUnit 5, Mockito.

---

### Task 1: Define outbox contract and failing tests

**Files:**
- Modify: `notification-service/src/test/java/com/notificationhub/notification/application/CreateNotificationServiceTest.java`
- Create: `notification-service/src/test/java/com/notificationhub/notification/application/NotificationOutboxDispatcherTest.java`
- Create: `notification-service/src/test/java/com/notificationhub/notification/infrastructure/persistence/migration/NotificationOutboxMigrationTest.java`

**Step 1:** Create tests for outbox save, dispatcher success/failure state, and migration declarations.

**Step 2:** Run the focused tests and confirm they fail because the outbox contracts are missing.

### Task 2: Add outbox persistence

**Files:**
- Create: `notification-service/src/main/java/com/notificationhub/notification/domain/port/out/NotificationOutboxPort.java`
- Create: `notification-service/src/main/java/com/notificationhub/notification/infrastructure/persistence/entity/NotificationOutboxEntity.java`
- Create: `notification-service/src/main/java/com/notificationhub/notification/infrastructure/persistence/repository/NotificationOutboxJpaRepository.java`
- Create: `notification-service/src/main/java/com/notificationhub/notification/infrastructure/persistence/adapter/NotificationOutboxRepositoryAdapter.java`
- Create: `notification-service/src/main/resources/db/migration/V3__create_notification_outbox.sql`

**Step 1:** Add pending/published state and payload fields.

**Step 2:** Implement save and ordered pending lookup through the port.

### Task 3: Connect transaction and dispatcher

**Files:**
- Modify: `notification-service/src/main/java/com/notificationhub/notification/application/service/CreateNotificationService.java`
- Modify: `notification-service/src/main/java/com/notificationhub/notification/domain/port/out/NotificationEventPublisher.java`
- Modify: `notification-service/src/main/java/com/notificationhub/notification/infrastructure/messaging/KafkaNotificationEventPublisher.java`
- Create: `notification-service/src/main/java/com/notificationhub/notification/application/service/NotificationOutboxDispatcher.java`

**Step 1:** Save an outbox row in the notification creation transaction.

**Step 2:** Publish pending rows and mark only successful rows as published.

### Task 4: Verify and document

**Files:**
- Modify: `docs/04-report/portfolio-ppt.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1:** Run focused notification tests and full `mvn test`.

**Step 2:** Mark the Outbox follow-up as completed and record the Docker-dependent test limitation.

**Step 3:** Commit, push, create a PR, wait for CI, and merge into `main`.
