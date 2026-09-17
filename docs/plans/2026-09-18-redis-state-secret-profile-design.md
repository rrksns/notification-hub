# Redis 상태 일관성과 운영 시크릿 프로필 설계

## 목표

알림 생성 트랜잭션이 실패했을 때 Redis 쿼터와 idempotency 상태가 DB 상태보다 앞서 남지 않도록 보상 처리를 추가한다. 개발용 자격 증명은 `local` 프로필로 분리하고 운영 실행은 `production` 프로필에서 필수 환경변수를 사용하도록 한다.

## 범위

- 쿼터 소비 후 DB 또는 outbox 저장이 실패하면 쿼터를 한 건 되돌린다.
- idempotency 키 저장 후 후속 저장이 실패하면 키를 삭제한다.
- Redis 보상 연산은 원래 예외를 유지하고, 보상 자체의 실패는 로그로 남긴다.
- notification, delivery, user, analytics의 DB/Mongo 개발용 기본 자격 증명을 `application-local.yml`로 이동한다.
- Kubernetes 애플리케이션 Deployment에 `SPRING_PROFILES_ACTIVE=production`을 명시한다.
- JWT는 기존처럼 모든 프로필에서 필수 환경변수로 유지한다.

## 보상 규칙

1. idempotency 중복 확인을 수행한다.
2. Redis 쿼터를 소비한다.
3. DB notification과 outbox를 저장한다.
4. idempotency 키를 저장한다.
5. 3번 또는 4번 이후 예외가 발생하면 idempotency 삭제와 쿼터 release를 시도한다.
6. 보상 연산 실패가 원래 DB 예외를 대체하지 않도록 한다.

쿼터 release는 동일한 월별 키를 원자적으로 감소시키며, 값이 1 이하이면 키를 삭제한다. 동시 요청에서도 소비와 release의 합산 결과가 실제 성공 요청 수와 일치하도록 Redis Lua script로 처리한다.

## 운영 프로필

- `local`은 개발용 localhost와 disposable 기본 자격 증명을 제공한다.
- `production`은 local 기본 프로필을 로드하지 않으며 `DB_USERNAME`, `DB_PASSWORD`, `MONGO_USER`, `MONGO_PASSWORD`, `JWT_SECRET` 같은 실행 환경 값을 요구한다.
- Kubernetes Secret과 ConfigMap은 production 프로필에서 해당 환경변수를 주입한다.

## 검증 기준

- DB 저장 또는 outbox 저장 실패 시 Redis 보상 메서드가 호출된다.
- 보상 실패가 원래 예외를 가리지 않는다.
- local 프로필은 기존 Docker Compose 개발 환경으로 기동할 수 있다.
- production 프로필은 필수 시크릿 누락을 fallback 없이 실패시킨다.
- 전체 Maven 테스트와 Docker E2E가 통과한다.
