# DLQ 운영 도구 설계

## 배경

상용화 우선순위 P1 5번은 `DLQ 운영 도구 추가`다. 현재 `delivery-service`는 `notifications.dlq` 토픽을 소비해 로그만 남기므로, 운영자가 실패 메시지를 확인하고 재처리할 수 있는 안전한 표면이 부족하다.

이번 작업은 직전 E2E 테스트 브랜치 `origin/feat/e2e-integration-tests` 위에 쌓는다. 해당 브랜치가 아직 `main`에 병합되지 않았고, DLQ 도구 구현 전에 Docker 기반 통합 테스트 기준선이 이미 확보되어 있기 때문이다.

## 목표

운영자가 로컬 또는 배포 환경에서 `notifications.dlq` 메시지를 조회, 파일로 내보내기, 파일에서 재처리할 수 있는 CLI를 제공한다. 기본 동작은 파괴적이지 않아야 하며, 실제 재발행은 명시적인 실행 플래그가 있을 때만 수행한다.

## 선택한 접근

별도 Maven 모듈 `dlq-ops`를 추가한다.

이 접근은 `common` 모듈의 `NotificationEvent`를 그대로 재사용할 수 있고, 서비스 코드에서 이미 사용하는 Spring Kafka와 Jackson 구성을 같은 JVM 생태계 안에서 검증할 수 있다. 쉘 스크립트는 빠르지만 JSON 직렬화, 필터링, dry-run 검증을 테스트하기 어렵고 운영 실수 방지가 약하다. API 방식은 장기적으로 유용하지만 지금은 인증, 권한, 배포 표면이 추가되어 P1 작업의 범위를 키운다.

## CLI 범위

초기 명령은 세 가지로 제한한다.

| 명령 | 역할 |
|---|---|
| `list` | DLQ 토픽을 읽고 조건에 맞는 메시지 요약을 출력한다 |
| `export` | DLQ 메시지를 JSON Lines 파일로 저장한다 |
| `replay` | export 파일을 읽어 `notifications` 토픽으로 재발행한다 |

공통 옵션은 `--bootstrap-servers`, `--topic`, `--group-id`, `--limit`, `--timeout-seconds`, `--tenant-id`, `--notification-id`, `--channel`로 둔다. `--topic` 기본값은 `notifications.dlq`이고 replay 대상 기본값은 `notifications`다.

## 안전 장치

DLQ 조회는 offset을 커밋하지 않는 일회성 consumer group으로 수행한다. 명시적인 `--group-id`가 없으면 `dlq-ops-<UUID>` 형식의 임시 group을 사용한다.

`replay`는 기본적으로 dry-run으로 동작한다. 실제 Kafka 재발행은 `--execute`가 전달될 때만 수행하고, dry-run 출력은 대상 토픽과 재발행 대상 메시지 수를 보여준다.

도구는 DLQ 메시지를 삭제하지 않는다. Kafka 토픽의 보존 정책이 메시지 수명 관리를 담당하고, 운영자는 export 파일을 통해 어떤 메시지를 재처리했는지 별도로 보관할 수 있다.

## 파일 형식

`export`는 JSON Lines 형식으로 한 줄에 한 Kafka 레코드를 저장한다. 각 줄은 원본 Kafka 메타데이터와 `NotificationEvent`를 포함한다.

```json
{"topic":"notifications.dlq","partition":0,"offset":12,"timestamp":"2026-08-26T12:00:00Z","key":"notification-1","event":{"notificationId":"notification-1","tenantId":"tenant-1","channel":"EMAIL","recipient":"user@example.com","content":"hello","idempotencyKey":"key-1","occurredAt":"2026-08-26T12:00:00Z"}}
```

## 테스트 전략

먼저 Kafka 없이 검증 가능한 경계를 테스트한다.

- CLI 인자 파싱과 기본값.
- 필터 조건 매칭.
- JSON Lines export/import round-trip.
- replay dry-run이 producer를 호출하지 않는 동작.

그 다음 Maven 모듈 테스트와 전체 reactor 테스트를 실행한다. 실제 Kafka smoke는 이번 구현의 필수 조건으로 두지 않는다. 이미 E2E 모듈이 Kafka/Testcontainers 기준선을 제공하고, 운영 CLI는 adapter 경계만 얇게 두면 단위 테스트로 대부분의 위험을 잠글 수 있기 때문이다.

## 제외 범위

- 웹 관리자 화면.
- 운영 API와 권한 모델.
- DLQ 메시지 삭제 또는 compact 처리.
- `delivery-results.dlq` 재처리.
- Provider별 실패 원인 분석 UI.
