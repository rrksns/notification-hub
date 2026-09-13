# Core E2E Integration Tests Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add automated E2E-style integration tests that exercise the core Notification Hub persistence and Kafka pipeline with real Testcontainers infrastructure.

**Architecture:** Add a dedicated `e2e-tests` Maven module that depends on the service modules under test. Keep the tests outside production modules so slow Docker-backed checks are easy to identify, while still running under the normal Maven reactor. Use Testcontainers for MySQL, Redis, Kafka, and MongoDB, and inject dynamic connection properties into the service Spring contexts.

**Tech Stack:** Java 17, Spring Boot 3.2.5, JUnit 5, Testcontainers, Spring Kafka test utilities, MySQL 8, Redis 7, Kafka, MongoDB 7.

---

### Task 1: Add E2E Test Module

**Files:**
- Modify: `pom.xml`
- Create: `e2e-tests/pom.xml`

**Step 1: Add module to root Maven reactor**

Add `e2e-tests` after `analytics-service` in the root `<modules>` list.

**Step 2: Create module POM**

Create a test-only jar module depending on:
- `notification-service`
- `delivery-service`
- `analytics-service`
- `common`
- `spring-boot-starter-test`
- `spring-boot-testcontainers`
- `spring-kafka-test`
- `org.testcontainers:testcontainers`
- `org.testcontainers:junit-jupiter`
- `org.testcontainers:mysql`
- `org.testcontainers:kafka`
- `org.testcontainers:mongodb`

**Step 3: Verify module wiring**

Run: `mvn test -pl e2e-tests -am -DskipTests`

Expected: module dependency graph compiles without running tests.

### Task 2: Add Notification Acceptance Integration Test

**Files:**
- Create: `e2e-tests/src/test/java/com/notificationhub/e2e/NotificationAcceptanceIntegrationTest.java`

**Step 1: Write the RED test**

The test must:
- start MySQL, Redis, and Kafka containers.
- start `NotificationServiceApplication` with web type `NONE`.
- call `CreateNotificationUseCase.create`.
- assert MySQL has the notification row.
- assert Redis has `idempotency:notification:{tenantId}:{idempotencyKey}`.
- assert Kafka `notifications` has a `NotificationEvent` for the created notification.

**Step 2: Run focused test**

Run: `mvn test -pl e2e-tests -am -Dtest=NotificationAcceptanceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected before dependencies and helper code settle: FAIL at compile or context startup.

**Step 3: Make minimal implementation pass**

Add only the helper methods required inside the test class:
- `@DynamicPropertySource` for datasource, Redis, Kafka, Eureka disabled, and web type none.
- Kafka topic creation via `AdminClient`.
- Kafka consumer helper using `JsonDeserializer<NotificationEvent>`.

**Step 4: Verify focused test**

Run the same focused Maven command.

Expected: PASS when Docker is available, or SKIP when Docker is unavailable.

### Task 3: Add Delivery and Analytics Pipeline Integration Test

**Files:**
- Create: `e2e-tests/src/test/java/com/notificationhub/e2e/DeliveryAnalyticsPipelineIntegrationTest.java`

**Step 1: Write the RED test**

The test must:
- start Kafka, MySQL, MongoDB, and Redis containers.
- create Kafka topics `notifications`, `notifications.dlq`, `delivery-results`, and `delivery-results.dlq`.
- start `DeliveryServiceApplication` and `AnalyticsServiceApplication` as separate non-web Spring contexts.
- publish a `NotificationEvent` to `notifications`.
- wait until delivery-service stores a `SUCCESS` delivery log in MySQL.
- wait until analytics-service stores a delivery event in MongoDB.
- wait until Redis `realtime:{tenantId}:success:total` becomes `1`.

**Step 2: Run focused test**

Run: `mvn test -pl e2e-tests -am -Dtest=DeliveryAnalyticsPipelineIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected before helper code settles: FAIL at compile or context startup.

**Step 3: Make minimal implementation pass**

Use programmatic `SpringApplicationBuilder` contexts so delivery and analytics can keep their own beans and Kafka listeners. Close both contexts in `@AfterAll`.

**Step 4: Verify focused test**

Run the same focused Maven command.

Expected: PASS when Docker is available, or SKIP when Docker is unavailable.

### Task 4: Document and Verify

**Files:**
- Modify: `README.md`
- Modify: `manual_test.md`
- Modify: `docs/plans/2026-08-19-commercialization-priority-list.md`
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: Update docs**

Document the new `e2e-tests` module, Testcontainers behavior, Docker skip behavior, and the verified P0 4 status.

**Step 2: Run verification**

Run:
- `mvn test -pl e2e-tests -am`
- `mvn test`
- `git diff --check`

Expected: all commands complete successfully. Docker-backed tests may be skipped only when Docker is unavailable.

**Step 3: Commit implementation**

Commit the E2E implementation and documentation together because the docs describe the new test surface.
