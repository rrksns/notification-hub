# 코드 리팩토링 및 상용화 개선 우선순위 설계

## 검토 기준

- 기준 커밋: `72010691926472d3166ac950de9fff86cedea066`
- 검토 범위: 애플리케이션 코드, 데이터베이스 경계, Kafka 처리, 배포·보안·백업 운영 문서.
- 제외 범위: iOS 구현 및 검증.
- 현재 검증: `mvn test` 성공, 161개 테스트 통과, Docker 미가동으로 E2E 2개 스킵.

## 발견사항

### P0. 배포와 운영 복구 경로가 자동 검증되지 않음

- `.github/workflows/ci.yml`은 이미지 빌드·푸시까지만 수행한다.
- README의 수동 배포·롤백 명령과 NetworkPolicy 실제 CNI 검증, 백업 외부 저장소·복원 환경은 운영 환경에서 별도 확인해야 한다.
- 구현보다 먼저 배포 후 헬스 체크, 롤백 기준, NetworkPolicy 허용·차단 시나리오, RPO/RTO 측정 결과를 남기는 설계가 필요하다.

### P0. 외부 노출·인증·시크릿 차단 항목

- `GET /api/deliveries/{id}`가 tenant 조건 없이 ID만으로 delivery log를 조회한다.
- API Gateway의 JWT fallback secret이 운영에서 노출되거나 누락되면 토큰 위조 위험이 있다.
- 로그인 rate limit과 계정 열거 방지 응답 정책이 필요하다.
- Kubernetes·AWS Kafka가 plaintext/비인증으로 구성되어 있고, public ingress TLS가 없다.
- Terraform DocumentDB master password가 소스에 포함되어 있다.
- API key는 생성·저장되지만 실제 gateway/service 인증 경로에 연결되어 있지 않다.

이 항목들은 리팩토링보다 먼저 외부 접근 차단과 자격 증명 교체를 설계해야 하는 상용화 차단 조건이다.

### P1. 알림 outbox가 다중 인스턴스에서 중복 발행될 수 있음

- `NotificationOutboxDispatcher`는 잠금 없이 pending 100건을 조회한 뒤 발행한다.
- 여러 인스턴스가 같은 행을 조회하면 Kafka 중복 이벤트가 발생할 수 있다.
- 현재 구조는 at-least-once를 의도하지만, 동시성 제어와 backlog 관측이 없다.

### P1. 발송 결과 이벤트가 DB 트랜잭션과 분리되어 유실될 수 있음

- `ProcessDeliveryService`는 delivery log 저장 이후 Kafka 결과 이벤트를 직접 발행한다.
- 코드 주석에도 DB 커밋 후 Kafka 발행 실패 시 analytics가 누락될 수 있음이 명시되어 있다.
- delivery-result outbox 또는 동일한 재발행 보장 전략을 별도 범위로 설계해야 한다.
- provider 발송 성공 후 결과 Kafka 발행이 실패하면 예외가 동일 트랜잭션으로 전파될 수 있다.
- DB 롤백 뒤 Kafka 재처리로 실제 provider 발송이 중복될 수 있으므로, 발송과 결과 기록의 상태 전이를 분리해야 한다.

### P1. 개인정보 보존 정책이 outbox와 delivery 기록까지 닿지 않음

- notification 본문·수신자는 notification-service 본 테이블에서만 90일 후 삭제된다.
- `notification_outbox`는 발행 완료 후에도 본문·수신자를 보존하며, 별도 정리 작업이 없다.
- outbox 발행 완료 행의 보존 기간과 delivery·analytics 기록의 보존 정책을 분리해 결정해야 한다.

### P1. 기본 자격 증명과 JWT 시크릿이 실행 설정에 남아 있음

- 서비스 YAML과 `docker-compose.yml`에 개발용 기본 비밀번호와 JWT fallback 값이 존재한다.
- 외부 환경변수 사용 경로는 있으나 운영 프로필에서 fallback을 금지하는 계약이 없다.
- 운영 실행 시 필수 시크릿 누락을 시작 단계에서 실패시키고, Compose 기본값은 로컬 전용임을 명시하거나 환경변수 필수값으로 바꿔야 한다.

### P1. 중복 처리와 데이터 무결성

- delivery log는 read-then-insert 방식이고 `notification_id` unique 제약이 없어 concurrent consumer에서 중복 row가 생길 수 있다.
- Redis quota/idempotency 기록은 DB 트랜잭션 rollback 뒤에도 남을 수 있어 정상 재시도를 막거나 quota를 소모할 수 있다.
- 월간 quota key의 TTL이 요청마다 갱신되는지 정책을 확인하고, 고정 만료 경계가 필요하면 월말 기준으로 변경한다.
- 감사 로그는 기록만 가능하며 조회·내보내기 운영 경로가 없다.

### P2. 알림 도메인 관측성과 문서 상태가 구현 상태를 충분히 반영하지 않음

- 현재 Prometheus 알림은 서비스 다운과 HTTP 5xx 중심이며 outbox backlog, Kafka lag, DLQ depth, provider failure rate가 없다.
- 상용화 우선순위 문서에는 완료된 기술 항목과 실제 운영 검증 대기 항목이 같은 표에 남아 있다.
- SendGrid·Android FCM은 검증 완료로 분리하고 Twilio·iOS는 미검증으로 표시해야 한다. iOS는 이번 작업에서 제외한다.

## 우선 구현 순서

1. **운영 출시 게이트 설계 및 검증.** 배포 smoke test, rollback 기준, NetworkPolicy 검증 시나리오, backup/restore RPO·RTO 증적을 문서와 CI 또는 운영 runbook에 반영한다.
2. **외부 접근과 자격 증명 차단.** tenant-scoped 조회, JWT fallback 제거, API key 인증 연결 여부 결정, TLS·Kafka 인증, DocumentDB secret 분리를 우선 처리한다.
3. **notification outbox 동시성·수명주기 개선.** claim/lease 또는 DB 잠금 전략을 선택하고, published 행 정리 정책과 backlog metric을 추가한다. 중복 발행 허용 범위와 소비자 idempotency를 테스트로 고정한다.
4. **delivery result outbox와 중복 방지.** 발송 로그와 결과 이벤트를 분리된 재발행 경계로 설계하고 `notification_id` unique 제약과 rollback 시나리오를 테스트한다.
5. **Redis 상태와 시크릿 실행 프로필 정리.** DB 트랜잭션과 Redis 기록의 일관성 전략을 정하고, 운영 fallback secret 제거 및 로컬 Compose 분리를 적용한다.
6. **관측성과 상태 문서 정리.** outbox·Kafka·DLQ·provider 지표와 알림을 추가하고, 완료·운영 검증 대기·제품 후속 작업을 문서에서 분리한다.

## 이번 설계의 완료 기준

- 각 항목에 변경 대상 파일, 데이터 마이그레이션 필요 여부, 실패 시 복구 방법, 테스트 시나리오가 정의되어 있다.
- iOS 작업은 우선순위 목록에서 명시적으로 제외되어 있다.
- 구현은 1번 운영 출시 게이트에 대한 사용자 승인 후 시작한다.

## 검토에서 통과한 범위

- 서비스 JWT 검증과 신뢰 헤더 덮어쓰기.
- Flyway 마이그레이션, 주요 FK·인덱스, notification transactional outbox 기본 경로.
- Mongo 원자 집계와 Redis Lua quota 소비.
- SendGrid, Twilio, FCM provider 어댑터.
- DLQ 조회·export·replay CLI.
- production source의 과도한 파일 크기나 테스트 조작 정황 없음.
