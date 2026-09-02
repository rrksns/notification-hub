# Notification Hub 상용화 우선순위

## 목적

현재 프로젝트를 포트폴리오 수준에서 실제 운영 서비스 수준으로 올릴 때 먼저 막아야 할 리스크를 정리한다.
기존 `docs/improvement-todo.md`는 코드 리뷰 기반 개선 목록이고, 이 문서는 상용 운영 준비 관점의 우선순위다.

## P0. 상용화 전 필수

| 순위 | 작업 | 이유 | 현재 근거 |
|---|---|---|---|
| 1 | 내부 서비스 직접 호출 차단 | api-gateway 우회 시 tenant header 위조와 무인증 직접 호출 가능성이 남는다 | user, notification, delivery, analytics `SecurityConfig`가 `permitAll()`이다 |
| 2 | K8s Secret 평문 제거 | 운영 secret이 Git에 평문으로 남으면 배포 전제 자체가 깨진다 | `k8s/secret.yaml` 추적 제거와 배포 시 Secret 생성 절차 필요 |
| 3 | DB 마이그레이션 체계 도입 | `DDL_AUTO=validate`만으로는 운영 스키마 변경, 롤백, 배포 순서를 관리할 수 없다 | Flyway migration 추가와 기존 DB 전환 절차 필요 |
| 4 | 핵심 E2E 통합 테스트 추가 | 단위 테스트만으로 Kafka, Redis, MySQL, Mongo 연동 장애를 잡기 어렵다 | `e2e-tests` 모듈에서 Testcontainers 기반 접수와 파이프라인 검증을 수행한다 |

## P1. 운영 안정성 강화

| 순위 | 작업 | 이유 | 권장 방향 |
|---|---|---|---|
| 5 | DLQ 운영 도구 추가 | 최종 실패 메시지가 로그에만 남으면 운영자가 재처리하기 어렵다 | `dlq-ops` CLI로 DLQ 조회, JSONL export, dry-run 기본 replay를 수행한다 |
| 6 | 알림/경보 체계 추가 | Prometheus/Grafana는 있으나 장애를 사람에게 알리는 Alertmanager 규칙이 필요하다 | 5xx, Kafka lag, DLQ 증가, 외부 provider 실패율 알림 |
| 7 | 이미지 배포 전략 정리 | CI는 이미지를 빌드만 하고 push/release promotion이 없다 | registry push, immutable tag, rollback 절차 |
| 8 | 백업과 복구 리허설 | MySQL, MongoDB, Redis/Kafka 상태 복구 절차가 운영 문서로 필요하다 | RPO/RTO, 백업 주기, 복구 테스트 기록 |

## P2. 비즈니스 운영 기능

| 순위 | 작업 | 이유 | 권장 방향 |
|---|---|---|---|
| 9 | 테넌트별 쿼터와 요금제 제한 | 멀티테넌트 SaaS에서는 과금과 abuse 방어가 필요하다 | plan별 발송량 제한, 월별 사용량 집계 |
| 10 | 감사 로그 | 관리자/API key 작업 추적이 필요하다 | tenant, actor, action, resource, timestamp 저장 |
| 11 | Provider fallback 정책 | SendGrid/Twilio/FCM 장애 시 운영 정책이 필요하다 | 대체 provider, 지연 발송, 고객 알림 정책 |
| 12 | 개인정보 보존/삭제 정책 | recipient와 content 보존 기간이 필요하다 | retention job, masking, delete workflow |

## 다음 실행 항목

4번 `핵심 E2E 통합 테스트 추가`를 진행했다.
P1 5번 `DLQ 운영 도구 추가`를 진행했다.
P1 6번 `알림/경보 체계 추가`를 진행했다.
P1 7번 `이미지 배포 전략 정리`를 진행했다.
P1 8번 `백업과 복구 리허설`의 운영 스크립트 구현을 진행했다.
별도 임시 환경에서 실제 복구 리허설을 수행한 뒤 P1 8번을 완료한다.
P2 9번 `테넌트별 쿼터와 요금제 제한`을 구현했다.
P2 10번 `감사 로그`를 구현했다. user-service에서 성공한 테넌트 등록, 로그인, API key 생성을 감사 로그로 저장한다.
P2 11번 `Provider fallback 정책`을 구현했다. delivery-service는 재시도 소진 후 실패를 운영 로그에 기록하고 FAILED 상태를 유지한다.
실제 보조 provider와 지연 발송, 고객 알림은 후속 범위로 남겼다.
다음 상용화 작업은 P2 12번 `개인정보 보존/삭제 정책`이다.
