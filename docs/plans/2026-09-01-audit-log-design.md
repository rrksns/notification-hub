# 감사 로그 설계

## 목표

관리자와 API key 관련 작업을 테넌트 단위로 추적할 수 있도록 user-service에 감사 로그를 저장한다.

## 범위

- 성공한 테넌트 등록을 `TENANT_REGISTERED`로 기록한다.
- 성공한 로그인을 `USER_LOGIN`으로 기록한다.
- 성공한 API key 생성을 `API_KEY_CREATED`로 기록한다.
- 로그인 실패, 비밀번호, API key 원문은 저장하지 않는다.
- 감사 로그 조회 API는 이번 범위에 포함하지 않는다.

## 저장 모델

`audit_logs` 테이블에 `id`, `tenant_id`, `actor_id`, `action`, `resource`, `occurred_at`을 저장한다.

등록 이벤트의 actor는 `system`으로 기록하고 로그인과 API key 생성은 사용자 ID를 기록한다. 저장 실패는 기존 user-service 트랜잭션 경계 안에서 처리한다.

## 검증 기준

- 각 성공 이벤트가 감사 로그 포트에 정확한 값을 전달한다.
- 중복 이메일과 잘못된 로그인은 감사 로그를 남기지 않는다.
- Flyway migration과 전체 Maven 테스트가 통과한다.
