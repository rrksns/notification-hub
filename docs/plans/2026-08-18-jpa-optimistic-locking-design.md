# JPA Optimistic Locking 설계

## 목표

JPA 엔티티에 `@Version` 필드를 추가해 동시 업데이트 시 마지막 저장이 이전 저장을 조용히 덮어쓰는 위험을 줄인다.

## 범위

- JPA 엔티티에만 `private Long version` 필드를 추가한다.
- 도메인 모델, DTO, mapper, API 응답은 변경하지 않는다.
- 마이그레이션 스크립트는 추가하지 않는다. 현재 로컬 기본 `DDL_AUTO=update` 정책과 기존 문서의 프로덕션 `validate` 권고는 유지한다.

## 대상 엔티티

- `user-service`: `TenantEntity`, `UserEntity`, `ApiKeyEntity`.
- `notification-service`: `NotificationEntity`.
- `delivery-service`: `DeliveryLogEntity`.

## 접근

- 각 엔티티에 `@Version private Long version;`을 추가한다.
- 생성자 시그니처와 mapper contract는 유지한다.
- 각 JPA 모듈에 reflection 기반 구조 테스트를 추가해 version 필드의 존재, `Long` 타입, `@Version` annotation을 검증한다.

## 성공 기준

- 모든 JPA 엔티티가 `@Version Long version` 필드를 가진다.
- 기존 도메인/API contract가 변하지 않는다.
- `mvn test -pl user-service,notification-service,delivery-service -am`과 `git diff --check`가 통과한다.
