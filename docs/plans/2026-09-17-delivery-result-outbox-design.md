# Delivery 결과 Outbox 설계

## 목표

`delivery-service`가 provider 발송 결과를 저장한 뒤 Kafka 결과 이벤트를 안정적으로 재발행할 수 있도록 한다. 동일한 `notification_id`에 대한 delivery log 중복 생성을 데이터베이스 제약으로 차단한다.

## 범위

- `delivery_logs.notification_id` 유니크 제약 추가.
- `delivery_result_outbox` 테이블과 JPA adapter 추가.
- 발송 로그와 결과 outbox를 같은 트랜잭션에서 저장.
- pending 결과 outbox를 Kafka `delivery-results` 토픽으로 발행하는 스케줄러 추가.
- Kafka 발행 실패 시 pending 상태를 유지해 다음 주기에 재시도.
- unit test와 Docker 기반 delivery-analytics E2E로 결과 이벤트 전달 검증.

## 제외 범위

- 실제 provider 발송 자체의 중복 방지.
- outbox claim lease와 다중 replica의 고급 배치 조정.
- delivery 및 analytics 보존 정책.
- iOS provider 구현.

## 상태 전이

1. notification 이벤트 수신.
2. `delivery_logs`에 PENDING 기록을 저장한다.
3. provider 발송 후 SUCCESS 또는 FAILED 로그와 동일한 결과 outbox를 저장한다.
4. 스케줄러가 PENDING outbox를 읽어 Kafka에 발행한다.
5. 발행 성공 시 PUBLISHED로 전환하고, 실패 시 PENDING을 유지한다.

## 복구 기준

- Kafka 발행 실패는 애플리케이션 예외로 provider 발송을 재수행하지 않는다.
- 결과 outbox가 남아 있으므로 다음 스케줄 주기에 analytics 이벤트를 재발행한다.
- Kafka acknowledgement 이후 상태 갱신 전에 프로세스가 중단되면 중복 이벤트가 발생할 수 있으므로 analytics 소비자는 기존과 같이 idempotency를 유지해야 한다.
- 이미 delivery log가 존재하는 notification 이벤트는 provider를 다시 호출하지 않고 기존 결과를 반환한다.

## 검증 기준

- 결과 outbox가 성공·실패 발송 결과와 함께 저장된다.
- Kafka 발행 실패 뒤 outbox가 PENDING으로 남는다.
- 동일 notification 이벤트의 동시 처리에서 DB 유니크 제약이 중복 row를 차단한다.
- Docker 기반 delivery-analytics E2E가 결과 이벤트와 analytics 집계를 확인한다.
