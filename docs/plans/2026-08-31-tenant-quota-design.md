# 테넌트별 쿼터와 요금제 제한 설계

## 목표

요금제별 월간 알림 발송량을 강제해 abuse를 제한하고, 초과 요청을 일관되게 `429 Too Many Requests`로 응답한다.

## 결정

- 요금제별 기본 월간 한도는 `FREE=100`, `BASIC=1000`, `PREMIUM=10000`, `ENTERPRISE=100000`으로 둔다.
- 로그인 시점에 user-service가 DB의 `SubscriptionPlan`을 JWT `plan` claim으로 서명한다.
- Gateway와 내부 서비스 JWT 필터만 검증된 plan을 `X-Tenant-Plan`으로 주입한다. 클라이언트가 보낸 plan 헤더는 제거한다.
- notification-service는 Redis Lua script의 원자적 증가와 한도 비교로 동시 요청을 안전하게 제한한다.
- 카운터 키는 `quota:notification:{tenantId}:{yyyy-MM}`이고 월이 바뀌면 새 키를 사용한다.
- 중복 idempotency 요청은 쿼터를 소비하지 않는다. 쿼터 검사는 중복 검사 뒤에 수행한다.

## 검증

- JWT plan claim이 생성되고 내부 필터가 신뢰 헤더로 재주입되는지 검증한다.
- 요금제 한도 계산과 월별 Redis 키 생성을 단위 테스트한다.
- 한도 초과 시 저장·Kafka publish가 실행되지 않고 `QUOTA_EXCEEDED`가 발생하는지 검증한다.
- 기존 알림 생성 및 전체 Maven 테스트를 실행한다.
