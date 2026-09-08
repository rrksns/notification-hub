# Notification Outbox 설계

**목표**: 알림 데이터 저장과 Kafka 이벤트 발행 사이의 장애 창을 제거해, 저장 성공 후 Kafka 발행이 실패해도 재시도할 수 있도록 한다.

## 선택한 범위

- notification-service의 `notifications` 저장 트랜잭션에 `notification_outbox` row 기록을 포함한다.
- 별도 스케줄러가 `PENDING` row를 시간순으로 읽어 Kafka `notifications` 토픽에 발행한다.
- Kafka 발행 성공 시에만 outbox 상태를 `PUBLISHED`로 변경한다.
- 기존 idempotency와 quota 동작은 유지한다.
- delivery-service의 결과 이벤트는 이번 범위에서 변경하지 않는다.

## 데이터 흐름

1. `CreateNotificationService`가 notification과 outbox row를 같은 JPA 트랜잭션으로 저장한다.
2. `NotificationOutboxDispatcher`가 pending row를 조회한다.
3. `KafkaNotificationEventPublisher`가 outbox payload를 Kafka에 동기 발행한다.
4. 성공하면 row를 `PUBLISHED`로 저장하고, 실패하면 `PENDING` 상태를 유지해 다음 주기에 재시도한다.

## 실패 및 중복 처리

- Kafka 발행 성공과 outbox 상태 변경 사이에 프로세스가 종료되면 같은 이벤트가 재발행될 수 있다.
- 따라서 outbox는 at-least-once 전달을 보장하고, 기존 notification idempotency key를 payload에 유지한다.
- `notification_id` unique 제약으로 동일 알림의 outbox row 중복 생성을 막는다.
- 이번 범위에서는 시도 횟수 제한과 별도 dead-letter outbox를 추가하지 않는다.

## 검증

- 서비스 테스트에서 직접 Kafka publisher를 호출하지 않고 outbox 저장 port를 호출하는지 확인한다.
- dispatcher 테스트에서 성공 시 `PUBLISHED`, 실패 시 `PENDING` 유지와 재throw를 확인한다.
- Flyway migration contract test와 전체 Maven 테스트를 실행한다.
