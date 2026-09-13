# DLQ Ops Tool Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a local CLI that can list, export, and safely replay `notifications.dlq` messages.

**Architecture:** Add a dedicated `dlq-ops` Maven module that depends on `common` and Spring Kafka. Keep CLI parsing, filtering, JSON Lines serialization, and Kafka adapter boundaries separate enough to test without Kafka for the first implementation pass.

**Tech Stack:** Java 17, Maven, Spring Kafka, Jackson, JUnit 5.

---

### Task 1: Module And CLI Skeleton

**Files:**
- Modify: `pom.xml`
- Create: `dlq-ops/pom.xml`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqOpsApplication.java`
- Create: `dlq-ops/src/test/java/com/notificationhub/dlqops/DlqOpsApplicationTest.java`

**Step 1: Write the failing smoke test**

Create a test that calls `DlqOpsApplication.run(new String[] {"--help"})` and expects exit code `0`.

**Step 2: Run the focused test and confirm RED**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOpsApplicationTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: Maven fails because `dlq-ops` is not yet a module or the application class does not exist.

**Step 3: Add the module and minimal CLI**

Add `dlq-ops` to the root modules list. Add a module POM with dependencies on `common`, `spring-kafka`, and `spring-boot-starter-test`.

Implement `DlqOpsApplication` with a `main` method and a testable `run(String[] args)` method. Only `--help` and missing command behavior are required in this task.

**Step 4: Verify focused test**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOpsApplicationTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: focused test passes.

**Step 5: Commit**

Commit message: `feat: DLQ 운영 CLI 모듈 추가`

### Task 2: Argument Parsing And Filters

**Files:**
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqCommand.java`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqOptions.java`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqOptionsParser.java`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqEventFilter.java`
- Create: `dlq-ops/src/test/java/com/notificationhub/dlqops/DlqOptionsParserTest.java`
- Create: `dlq-ops/src/test/java/com/notificationhub/dlqops/DlqEventFilterTest.java`

**Step 1: Write parser and filter tests**

Cover defaults for `list`, explicit `export`, explicit `replay`, unknown command, missing option value, and filters for tenant id, notification id, and channel.

**Step 2: Run focused tests and confirm RED**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOptionsParserTest,DlqEventFilterTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: tests fail because parser and filter classes do not exist.

**Step 3: Implement minimal parser and filter**

Keep parsing manual and deterministic. Do not add a CLI dependency.

Defaults:
- topic: `notifications.dlq`.
- target topic: `notifications`.
- limit: `50`.
- timeout seconds: `5`.
- execute: `false`.
- group id: generated `dlq-ops-<UUID>` if omitted.

**Step 4: Verify focused tests**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOptionsParserTest,DlqEventFilterTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: focused tests pass.

**Step 5: Commit**

Commit message: `feat: DLQ CLI 옵션 파싱 추가`

### Task 3: JSON Lines Export Model

**Files:**
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqRecord.java`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqRecordCodec.java`
- Create: `dlq-ops/src/test/java/com/notificationhub/dlqops/DlqRecordCodecTest.java`

**Step 1: Write codec tests**

Cover serialize one record, deserialize one record, and round-trip multiple JSON Lines records with `NotificationEvent`.

**Step 2: Run focused codec test and confirm RED**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqRecordCodecTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: tests fail because codec classes do not exist.

**Step 3: Implement codec**

Use Jackson from Spring Boot dependency management. Preserve Kafka metadata fields `topic`, `partition`, `offset`, `timestamp`, `key`, and `event`.

**Step 4: Verify focused codec test**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqRecordCodecTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: focused test passes.

**Step 5: Commit**

Commit message: `feat: DLQ JSONL export 형식 추가`

### Task 4: List And Export Commands

**Files:**
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqConsumerClient.java`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/KafkaDlqConsumerClient.java`
- Modify: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqOpsApplication.java`
- Create: `dlq-ops/src/test/java/com/notificationhub/dlqops/DlqOpsListExportTest.java`

**Step 1: Write command tests with fake consumer**

Verify `list` prints matching records and `export --output <file>` writes JSON Lines for matching records.

**Step 2: Run focused command tests and confirm RED**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOpsListExportTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: tests fail because command execution is not implemented.

**Step 3: Implement consumer port and Kafka adapter**

Use `KafkaConsumer<String, NotificationEvent>` with `JsonDeserializer<NotificationEvent>`, `enable.auto.commit=false`, `auto.offset.reset=earliest`, and a generated group id by default.

**Step 4: Verify focused command tests**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOpsListExportTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: focused tests pass.

**Step 5: Commit**

Commit message: `feat: DLQ 조회와 export 명령 추가`

### Task 5: Replay Command

**Files:**
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqReplayClient.java`
- Create: `dlq-ops/src/main/java/com/notificationhub/dlqops/KafkaDlqReplayClient.java`
- Modify: `dlq-ops/src/main/java/com/notificationhub/dlqops/DlqOpsApplication.java`
- Create: `dlq-ops/src/test/java/com/notificationhub/dlqops/DlqOpsReplayTest.java`

**Step 1: Write replay tests**

Verify dry-run does not publish, `--execute` publishes each exported event to `notifications`, and replay fails clearly when `--input` is missing.

**Step 2: Run focused replay tests and confirm RED**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOpsReplayTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: tests fail because replay is not implemented.

**Step 3: Implement replay port and Kafka adapter**

Use `KafkaProducer<String, NotificationEvent>` with `JsonSerializer<NotificationEvent>`. Use the original key when present, otherwise use `event.notificationId()` as the publish key.

**Step 4: Verify focused replay tests**

Run: `mvn test -pl dlq-ops -am -Dtest=DlqOpsReplayTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: focused tests pass.

**Step 5: Commit**

Commit message: `feat: DLQ replay 명령 추가`

### Task 6: Documentation And Full Verification

**Files:**
- Modify: `README.md`
- Modify: `manual_test.md`
- Modify: `docs/kafka-redis.md`
- Modify: `docs/plans/2026-08-19-commercialization-priority-list.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: Document operator usage**

Add examples for `list`, `export`, dry-run `replay`, and `replay --execute`.

**Step 2: Run full verification**

Run: `mvn test`

Expected: full reactor passes.

**Step 3: Manually exercise CLI surface**

Run: `mvn -pl dlq-ops -am exec:java -Dexec.mainClass=com.notificationhub.dlqops.DlqOpsApplication -Dexec.args=--help`

Expected: CLI help text prints and process exits with code `0`.

Run one invalid command.

Expected: CLI prints an error and exits non-zero.

**Step 4: Commit**

Commit message: `docs: DLQ 운영 도구 사용법 추가`
