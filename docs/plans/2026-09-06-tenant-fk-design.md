# Tenant FK 참조 무결성 설계

**목표**: `users.tenant_id`와 `api_keys.tenant_id`가 항상 존재하는 `tenants.id`를 참조하도록 데이터베이스 수준의 참조 무결성을 보장한다.

## 범위

- 기존 문자열 `tenantId` 필드와 도메인 모델은 유지한다.
- user-service Flyway에 FK 제약을 추가한다.
- `audit_logs.tenant_id`는 감사 로그 보존과 독립적인 기록 특성을 고려해 이번 범위에서 제외한다.
- 기존 데이터베이스에 이미 고아 레코드가 있는 경우 마이그레이션 전에 운영 데이터 점검이 필요하다.

## 제약 조건

- `users.tenant_id` → `tenants.id`
- `api_keys.tenant_id` → `tenants.id`
- 테넌트 삭제는 기본 동작을 유지하며, 명시적인 cascade 삭제 정책은 추가하지 않는다.

## 검증

- Flyway SQL에 FK 이름과 참조 대상이 선언되었는지 회귀 테스트로 확인한다.
- user-service 집중 테스트와 전체 Maven 테스트를 실행한다.
