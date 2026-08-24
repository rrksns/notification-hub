# DB Migration Design

## 목적

상용 운영에서 Hibernate `ddl-auto`에 의존하지 않고 서비스별 MySQL 스키마 변경 이력을 코드로 관리한다.

## 선택안

권장안은 Flyway다.
Spring Boot classpath 자동 설정을 그대로 사용하고, 각 JPA 서비스가 자신의 `src/main/resources/db/migration` 아래 SQL을 실행한다.

대안으로 Liquibase를 사용할 수 있지만 현재 스키마는 단순한 초기 테이블 생성 위주라 changelog 구조가 더 크다.
기존 `docker/mysql/init.sql` 중심 운영은 DB 생성과 권한 부여에는 충분하지만 서비스별 테이블 이력을 남기지 못한다.

## 범위

적용 대상은 MySQL을 사용하는 `user-service`, `notification-service`, `delivery-service`다.
MongoDB를 사용하는 `analytics-service`는 이번 작업 범위에서 제외한다.

## 구조

각 서비스 POM에 `org.flywaydb:flyway-mysql`을 추가한다.
각 서비스 `application.yml`은 `spring.jpa.hibernate.ddl-auto` 기본값을 `validate`로 변경하고, Flyway를 기본 활성화한다.
초기 migration은 `V1__create_*_schema.sql` 파일로 둔다.

## 전환 절차

빈 DB에서는 서비스 시작 시 Flyway가 V1 migration을 적용하고 Hibernate가 schema validate를 수행한다.
이미 Hibernate `ddl-auto=update`로 만들어진 DB는 바로 V1을 실행하면 충돌할 수 있다.
그 경우 운영자는 기존 schema를 점검한 뒤 `FLYWAY_BASELINE_ON_MIGRATE=true`로 최초 1회 baseline을 수행하거나 별도 데이터 이관 절차를 선택해야 한다.

## 검증

기본 검증은 전체 `mvn test`다.
가능하면 로컬 MySQL을 띄운 뒤 각 서비스가 migration과 schema validation을 통과하는지 확인한다.
