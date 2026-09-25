# Notification Outbox 다중 Replica 검증 계획

## 목표

동일한 MySQL과 Kafka를 공유하는 notification-service replica들이 하나의 pending outbox를 동시에 처리할 때 이벤트를 중복 발행하지 않는지 실제 인프라로 검증한다.

## 검증 방법

- Testcontainers로 MySQL, Redis, Kafka를 기동한다.
- 동일 인프라를 사용하는 notification-service Spring context를 두 개 기동한다.
- 테스트 스케줄러는 비활성화하고 각 context의 실제 transaction manager로 dispatcher 실행을 감싼다.
- 두 dispatcher를 동시에 실행한 뒤 Kafka `notifications` topic의 이벤트 수와 양쪽 outbox pending count를 확인한다.

## 구현 결정

- `NotificationOutboxDispatcher`는 `notification.outbox-scheduling.enabled`가 기본 `true`일 때만 Spring bean으로 등록된다.
- E2E는 자동 스케줄러 간섭을 막기 위해 해당 속성을 `false`로 설정하고, 실제 port·publisher·metrics bean을 주입한 dispatcher를 사용한다.
- 기존 PESSIMISTIC_WRITE 조회와 트랜잭션 경계가 한 replica가 행을 처리하는 동안 다른 replica의 조회를 직렬화한다.

## 완료 조건

1. 두 replica가 같은 pending row를 동시에 조회·발행한다.
2. Kafka topic에 해당 notification 이벤트가 정확히 1건 기록된다.
3. 두 replica에서 확인한 pending outbox count가 0이다.
4. focused E2E, 전체 Maven, 정적 release gate가 통과한다.
