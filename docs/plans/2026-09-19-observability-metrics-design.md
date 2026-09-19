# Outbox 관측성 지표 설계

## 목표

notification-service와 delivery-service의 outbox 적체와 Kafka 발행 실패를 Prometheus에서 확인할 수 있게 한다.

## 범위

- notification outbox pending 건수 gauge.
- notification outbox Kafka publish 실패 counter.
- delivery result outbox pending 건수 gauge.
- delivery result outbox Kafka publish 실패 counter.
- 기존 Prometheus 경보와 Grafana 대시보드에 새 지표를 연결한다.

## 설계

각 outbox repository에 pending 건수 조회를 추가한다. dispatcher는 기존 batch 조회와 발행 동작을 유지하고, 한 번의 dispatch 실행이 끝난 뒤 실제 pending 건수를 조회해 gauge를 갱신한다. 발행 예외는 기존처럼 outbox를 pending으로 남기고 publish 실패 counter만 증가시킨다.

Prometheus 지표 이름은 다음과 같다.

| Micrometer 이름 | Prometheus 이름 | 의미 |
|---|---|---|
| `notification.outbox.backlog` | `notification_outbox_backlog` | notification pending 건수 |
| `notification.outbox.publish.failure` | `notification_outbox_publish_failure_total` | notification 발행 실패 누적 건수 |
| `delivery.result.outbox.backlog` | `delivery_result_outbox_backlog` | delivery result pending 건수 |
| `delivery.result.outbox.publish.failure` | `delivery_result_outbox_publish_failure_total` | delivery result 발행 실패 누적 건수 |

## 검증 기준

- publish 성공 시 outbox 상태 전이는 기존과 동일하다.
- publish 실패 시 상태 전이는 없고 실패 counter가 1 증가한다.
- dispatch 종료 후 repository의 실제 pending 건수가 gauge에 반영된다.
- Prometheus rule과 dashboard JSON이 유효하고 전체 Maven 및 Docker E2E가 통과한다.
