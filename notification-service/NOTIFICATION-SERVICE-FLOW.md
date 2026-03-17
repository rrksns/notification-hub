# Notification Service 전체 플로우

## 역할

알림 요청을 수신하고, 중복을 방지(Idempotency)한 뒤 Kafka 이벤트를 발행하는 서비스입니다. 실제 발송은 delivery-service가 담당합니다.

---

## 디렉토리 구조

```
notification-service/
├── pom.xml
└── src/main/
    ├── java/com/notificationhub/notification/
    │   ├── NotificationServiceApplication.java
    │   ├── presentation/
    │   │   ├── controller/NotificationController.java
    │   │   └── dto/CreateNotificationRequest.java
    │   ├── application/service/
    │   │   ├── CreateNotificationService.java
    │   │   └── GetNotificationService.java
    │   ├── domain/
    │   │   ├── model/
    │   │   │   ├── Notification.java
    │   │   │   ├── Channel.java          (EMAIL, SMS, PUSH)
    │   │   │   └── NotificationStatus.java (PENDING, PUBLISHED, FAILED)
    │   │   ├── exception/InvalidNotificationException.java
    │   │   └── port/
    │   │       ├── in/
    │   │       │   ├── CreateNotificationUseCase.java
    │   │       │   └── GetNotificationUseCase.java
    │   │       └── out/
    │   │           ├── NotificationRepository.java
    │   │           ├── NotificationEventPublisher.java
    │   │           └── IdempotencyPort.java
    │   └── infrastructure/
    │       ├── persistence/
    │       │   ├── entity/NotificationEntity.java
    │       │   ├── repository/NotificationJpaRepository.java
    │       │   └── adapter/NotificationRepositoryAdapter.java
    │       ├── messaging/
    │       │   ├── KafkaConfig.java
    │       │   └── KafkaNotificationEventPublisher.java
    │       ├── cache/RedisIdempotencyAdapter.java
    │       └── security/SecurityConfig.java
    └── resources/application.yml
```

---

## 도메인 모델

### Notification (핵심 엔티티)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | UUID | 고유 식별자 |
| `tenantId` | String | 멀티 테넌시 식별자 |
| `channel` | Channel | EMAIL / SMS / PUSH |
| `recipient` | String | 수신자 주소 |
| `content` | String | 메시지 내용 (최대 2000자) |
| `idempotencyKey` | String | 중복 방지 키 (UNIQUE) |
| `status` | NotificationStatus | PENDING → PUBLISHED / FAILED |
| `createdAt` | Instant | 생성 시각 |

**상태 전이:** `PENDING` → `publish()` 호출 → `PUBLISHED`
이미 PUBLISHED 또는 FAILED 상태에서 publish() 호출 시 `InvalidNotificationException` 발생

---

## 주요 플로우

### 1. 알림 생성 (`POST /api/notifications`)

```
HTTP POST /api/notifications
  Header: X-Tenant-Id
  Body: { channel, recipient, content, idempotencyKey }
    ↓
NotificationController
  → CreateNotificationUseCase.Command 생성
    ↓
CreateNotificationService
  1. RedisIdempotencyAdapter.isDuplicate(key)
     └─ 중복이면 → BusinessException(DUPLICATE_NOTIFICATION)
  2. Notification.create() — 도메인 객체 생성 (상태: PENDING)
  3. notification.publish() — 상태 전이 (PENDING → PUBLISHED)
  4. NotificationRepositoryAdapter.save() → MySQL 저장
  5. RedisIdempotencyAdapter.save(key) — TTL 24시간
  6. KafkaNotificationEventPublisher.publish()
     → Kafka 토픽 "notifications" 발행 (key: tenantId)
    ↓
응답: { notificationId, status: "PUBLISHED" }
```

### 2. 알림 조회 (`GET /api/notifications/{id}`)

```
HTTP GET /api/notifications/{id}
  Header: X-Tenant-Id
    ↓
NotificationController
    ↓
GetNotificationService
  → findByIdAndTenantId(id, tenantId)  ← 테넌트 격리 적용
  → 없으면 BusinessException(NOTIFICATION_NOT_FOUND)
    ↓
응답: Notification 도메인 객체
```

---

## Kafka 이벤트 발행

- **토픽:** `notifications`
- **파티션 키:** `tenantId` (테넌트 단위 순서 보장)
- **이벤트 타입:** `NotificationEvent` (common 모듈 공유)
- **직렬화:** `JsonSerializer`

delivery-service가 이 토픽을 구독하여 실제 발송을 처리합니다.

---

## 중복 방지 (Idempotency)

- 클라이언트가 `idempotencyKey`를 요청에 포함
- Redis에 `idempotency:notification:{key}` 형태로 저장 (TTL 24시간)
- 동일 키로 재요청 시 즉시 거부 → 중복 발송 방지

---

## DB 스키마

```sql
CREATE TABLE notifications (
    id            VARCHAR(36)   PRIMARY KEY,
    tenantId      VARCHAR(255)  NOT NULL,
    channel       VARCHAR(20)   NOT NULL,  -- EMAIL / SMS / PUSH
    recipient     VARCHAR(255)  NOT NULL,
    content       VARCHAR(2000) NOT NULL,
    idempotencyKey VARCHAR(255) NOT NULL UNIQUE,
    status        VARCHAR(20)   NOT NULL,  -- PENDING / PUBLISHED / FAILED
    createdAt     DATETIME      NOT NULL
);
```

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 서버 포트 | 8082 |
| DB | MySQL (포트 3307, DB: notification_service) |
| 캐시 | Redis (localhost:6379) |
| 메시지 큐 | Kafka (localhost:9092) |
| 서비스 등록 | Eureka (localhost:8761) |
| 아키텍처 | 헥사고날 (Ports & Adapters) |
