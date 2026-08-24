# DB Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** user, notification, delivery 서비스의 MySQL 스키마를 Flyway migration으로 관리한다.

**Architecture:** 각 서비스가 자신의 classpath `db/migration` SQL을 실행한다. Hibernate는 Flyway 적용 후 `ddl-auto=validate`로 엔티티와 DB 스키마 불일치를 감지한다.

**Tech Stack:** Spring Boot 3.2.5, Spring Data JPA, Flyway, MySQL 8.

---

### Task 1: Flyway 의존성과 설정 추가

**Files:**
- Modify: `user-service/pom.xml`
- Modify: `notification-service/pom.xml`
- Modify: `delivery-service/pom.xml`
- Modify: `user-service/src/main/resources/application.yml`
- Modify: `notification-service/src/main/resources/application.yml`
- Modify: `delivery-service/src/main/resources/application.yml`

**Steps:**
1. 세 서비스에 `org.flywaydb:flyway-mysql` dependency를 추가한다.
2. 세 서비스의 `DDL_AUTO` 기본값을 `validate`로 바꾼다.
3. `spring.flyway.enabled`, `spring.flyway.locations`, `spring.flyway.baseline-on-migrate` 설정을 추가한다.
4. `mvn test`로 의존성 해석과 기존 테스트를 확인한다.

### Task 2: 초기 migration SQL 추가

**Files:**
- Create: `user-service/src/main/resources/db/migration/V1__create_user_schema.sql`
- Create: `notification-service/src/main/resources/db/migration/V1__create_notification_schema.sql`
- Create: `delivery-service/src/main/resources/db/migration/V1__create_delivery_schema.sql`

**Steps:**
1. 엔티티의 table, column, unique, index 정의를 SQL로 옮긴다.
2. Hibernate 기본 physical naming에 맞춰 camelCase 필드는 snake_case 컬럼으로 작성한다.
3. MySQL 8 기준 `utf8mb4_unicode_ci`를 사용한다.
4. 가능한 경우 로컬 MySQL에서 migration 적용과 Hibernate validation을 확인한다.

### Task 3: 운영 문서 갱신

**Files:**
- Modify: `README.md`
- Modify: `PROCESS.md`
- Modify: `docs/improvement-todo.md`
- Modify: `docs/plans/2026-08-19-commercialization-priority-list.md`
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. `DDL_AUTO=validate`와 Flyway 적용 순서를 README에 반영한다.
2. 기존 DB 전환 시 baseline 주의사항을 문서화한다.
3. 상용화 우선순위 문서의 다음 작업을 P0 4번으로 갱신한다.
4. 검증 결과를 manual test와 context notes에 남긴다.

### Task 4: 검증과 커밋

**Files:**
- All modified files.

**Steps:**
1. `mvn test`를 실행한다.
2. SQL 파일 첫 줄 주석과 migration 파일명 규칙을 확인한다.
3. `git diff --check`를 실행한다.
4. 변경을 커밋한다.
