# Kafka & Redis 동작 정리

---

## 1. Kafka

### 1.1 인프라 구성

- **이미지**: `apache/kafka:3.7.0`
- **모드**: KRaft (ZooKeeper 없음) — `KAFKA_PROCESS_ROLES: broker,controller`를 한 노드에서 모두 수행
- **포트**: 9092 (클라이언트), 9093 (컨트롤러 내부용)
- **직렬화**: Key — `StringSerializer`, Value — `JsonSerializer` / `JsonDeserializer`
- **Consumer 오프셋 정책**: `AUTO_OFFSET_RESET_CONFIG: earliest` — 서비스가 재시작되면 마지막 커밋 오프셋부터, 오프셋이 없으면 가장 오래된 메시지부터 소비

### 1.2 토픽 구성

`docker-compose.yml`의 `kafka-init` 컨테이너가 Kafka healthy 상태 후 자동 생성합니다.

| 토픽 | 파티션 | 발행 서비스 | 소비 서비스 | 용도 |
|------|--------|-----------|-----------|------|
| `notifications` | 3 | notification-service | delivery-service | 알림 발송 요청 |
| `notifications-retry-*` | 자동 | delivery-service | delivery-service | 재시도 (1s, 2s 지연) |
| `notifications.dlq` | 1 | delivery-service | — | 3회 실패 메시지 보관 |
| `delivery-results` | 3 | delivery-service | analytics-service | 발송 결과 집계 |
| `delivery-results.dlq` | 1 | — | — | (예비) 결과 처리 실패 보관 |

> `notifications-retry-*` 토픽은 `@RetryableTopic` 어노테이션이 런타임에 자동 생성합니다.
> `notifications`, `delivery-results` 토픽의 파티션 수가 3이므로 Consumer를 최대 3개까지 병렬 처리 가능합니다.

### 1.3 이벤트 페이로드

두 이벤트 모두 `common` 모듈에 Java record로 정의되어 있어 서비스 간 공유됩니다.

**`NotificationEvent`** — `notifications` 토픽에 발행

```java
// common/src/.../event/NotificationEvent.java
public record NotificationEvent(
    String notificationId,    // 알림 고유 ID (UUID)
    String tenantId,          // 테넌트 ID (Kafka 파티션 키로 사용)
    String channel,           // "EMAIL" | "SMS" | "PUSH"
    String recipient,         // 수신자 주소 (이메일, 전화번호 등)
    String content,           // 알림 본문
    String idempotencyKey,    // 멱등성 키
    Instant occurredAt        // 이벤트 발생 시각 (Instant.now())
)
```

**`DeliveryResultEvent`** — `delivery-results` 토픽에 발행

```java
// common/src/.../event/DeliveryResultEvent.java
public record DeliveryResultEvent(
    String deliveryLogId,     // 발송 로그 ID (UUID)
    String notificationId,    // 원본 알림 ID
    String tenantId,          // 테넌트 ID (Kafka 파티션 키로 사용)
    String channel,           // "EMAIL" | "SMS" | "PUSH"
    String status,            // "SUCCESS" | "FAILED"
    String failureReason,     // 실패 사유 (성공 시 null)
    Instant occurredAt        // 이벤트 발생 시각
) {
    // 팩토리 메서드
    static DeliveryResultEvent success(...)  // status="SUCCESS", failureReason=null
    static DeliveryResultEvent failure(...)  // status="FAILED", failureReason=사유
}
```

---

### 1.4 흐름 1 — 알림 접수 → Kafka 발행 (notification-service)

**트리거**: 클라이언트가 `POST /api/notifications` 호출

```
클라이언트
  └─ POST /api/notifications { channel, recipient, content, idempotencyKey }
       └─ CreateNotificationService.create(command)
            │
            │ ① Redis 멱등성 체크 (→ 중복이면 여기서 409 반환, Kafka 발행 안 함)
            │ ② Notification 도메인 객체 생성 (상태: PENDING)
            │ ③ notification.publish() → 상태를 PUBLISHED로 변경
            │ ④ MySQL에 저장 (JPA 어댑터)
            │ ⑤ Redis에 멱등성 키 저장 (TTL 24h)
            │ ⑥ Kafka 발행 ↓
            │
            └─ KafkaNotificationEventPublisher.publish(notification)
                 └─ kafkaTemplate.send("notifications", tenantId, NotificationEvent)
                                                         ~~~~~~
                                                         파티션 키
```

**왜 `tenantId`를 파티션 키로 사용하는가?**

- 같은 테넌트의 메시지는 항상 같은 파티션으로 전달 → 테넌트 단위로 메시지 순서 보장
- 파티션 3개이므로 서로 다른 테넌트의 메시지는 병렬로 처리 가능

**Producer 설정** (`notification-service`):

```java
// KafkaConfig.java
ProducerConfig.BOOTSTRAP_SERVERS_CONFIG  → localhost:9092
ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG → StringSerializer
ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG → JsonSerializer
```

---

### 1.5 흐름 2 — Kafka 소비 → 채널 발송 + 재시도 (delivery-service)

**트리거**: `notifications` 토픽에 메시지가 들어오면 자동 소비

```
Kafka: notifications 토픽
  └─ NotificationEventConsumer.consume(NotificationEvent)
       │
       │  @RetryableTopic(attempts=3, backoff=1s×2배, dltTopicSuffix=".dlq")
       │  @KafkaListener(topics="notifications", groupId="delivery-service")
       │
       └─ ProcessDeliveryService.process(command)
            │
            │ ① DeliveryLog 도메인 객체 생성 (상태: PENDING) → MySQL 저장
            │ ② ChannelDelivererAdapter.deliver(channel, recipient, content)
            │    └─ @CircuitBreaker + @Retry 적용
            │    └─ channel별 분기: sendEmail() / sendSms() / sendPush()
            │    └─ 현재는 log.info() 스텁 (실제 발송 안 함)
            │
            ├─ 성공 시:
            │    ③ log.markSuccess() → MySQL 상태 업데이트 (SUCCESS)
            │    ④ KafkaDeliveryResultPublisher.publishSuccess(log)
            │       └─ kafkaTemplate.send("delivery-results", tenantId, DeliveryResultEvent.success(...))
            │
            └─ 실패 시:
                 ③ log.markFailed(reason) → MySQL 상태 업데이트 (FAILED)
                 ④ KafkaDeliveryResultPublisher.publishFailure(log)
                    └─ kafkaTemplate.send("delivery-results", tenantId, DeliveryResultEvent.failure(...))
```

**재시도 동작 상세:**

`@RetryableTopic`은 Spring Kafka가 제공하는 비차단(non-blocking) 재시도 메커니즘입니다. 실패한 메시지를 별도 재시도 토픽으로 보내고, 지연 후 재소비합니다.

```
1회 실패 → notifications-retry-1000 토픽 (1초 후 재소비)
              │
              └─ 2회 실패 → notifications-retry-2000 토픽 (2초 후 재소비)
                               │
                               └─ 3회 실패 → notifications.dlq 토픽 (최종 포기, 수동 확인 필요)
```

| 설정 | 값 | 의미 |
|------|----|------|
| `attempts` | 3 | 최대 3회 시도 |
| `backoff.delay` | 1000ms | 첫 재시도 대기 시간 |
| `backoff.multiplier` | 2 | 지수 배수 (1s → 2s → ...) |
| `dltTopicSuffix` | `.dlq` | DLQ 토픽 접미사 |

**Circuit Breaker + Retry 설정** (`delivery-service/application.yml`):

`@RetryableTopic`과 별개로, 채널 발송 어댑터 자체에도 Resilience4j 기반 방어가 적용되어 있습니다.

```yaml
resilience4j:
  retry:
    instances:
      channelDelivery:
        max-attempts: 3                    # 어댑터 레벨 최대 3회 재시도
        wait-duration: 1s                  # 첫 재시도 대기
        exponential-backoff-multiplier: 2  # 1s → 2s → 4s
  circuitbreaker:
    instances:
      channelDelivery:
        sliding-window-size: 10            # 최근 10회 호출 기준 판단
        failure-rate-threshold: 50         # 50% 이상 실패 시 OPEN
        wait-duration-in-open-state: 10s   # OPEN 상태 10초 유지 후 HALF_OPEN
```

> **2단계 방어 구조**: Resilience4j @Retry가 어댑터 내부에서 즉시 재시도 → 그래도 실패하면 예외가 @RetryableTopic으로 전파 → 별도 토픽에서 지연 재시도 → 최종 실패 시 DLQ

**Circuit Breaker 상태 전이:**

```
CLOSED (정상)
  └─ 최근 10회 중 5회 이상 실패 → OPEN (차단)
       └─ 모든 호출 즉시 fallback 실행: log.warn("[FALLBACK] Circuit OPEN")
       └─ 10초 후 → HALF_OPEN (탐색)
            └─ 1회 호출 성공 → CLOSED (정상 복귀)
            └─ 1회 호출 실패 → OPEN (다시 차단)
```

**Consumer 설정** (`delivery-service`):

```java
// KafkaConsumerConfig.java
ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG  → localhost:9092
ConsumerConfig.GROUP_ID_CONFIG           → "delivery-service"
ConsumerConfig.AUTO_OFFSET_RESET_CONFIG  → "earliest"
JsonDeserializer.addTrustedPackages("*") → 모든 패키지의 JSON 역직렬화 허용
```

---

### 1.6 흐름 3 — 발송 결과 소비 → 통계 집계 (analytics-service)

**트리거**: `delivery-results` 토픽에 메시지가 들어오면 자동 소비

```
Kafka: delivery-results 토픽
  └─ DeliveryResultConsumer.consume(DeliveryResultEvent)
       │
       │  @KafkaListener(topics="delivery-results", groupId="analytics-service")
       │
       └─ RecordDeliveryEventService.record(command)
            │
            │ ① DeliveryEvent 도메인 객체 생성 → MongoDB delivery_events 컬렉션 저장
            │
            │ ② DailyStats 조회 (테넌트 + 날짜 기준)
            │    ├─ 있으면 → 기존 DailyStats 가져옴
            │    └─ 없으면 → DailyStats.create() 새로 생성
            │
            │ ③ 성공/실패 분기:
            │    ├─ SUCCESS → stats.recordSuccess(channel) + Redis INCR 성공 카운터
            │    └─ FAILED  → stats.recordFailure(channel) + Redis INCR 실패 카운터
            │
            │ ④ DailyStats → MongoDB daily_stats 컬렉션 저장 (upsert)
```

**Consumer 설정** (`analytics-service`):

```java
// KafkaConsumerConfig.java
ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG  → localhost:9092
ConsumerConfig.GROUP_ID_CONFIG           → "analytics-service"
ConsumerConfig.AUTO_OFFSET_RESET_CONFIG  → "earliest"
JsonDeserializer<DeliveryResultEvent>    → DeliveryResultEvent.class로 역직렬화
```

---

### 1.7 Kafka 전체 메시지 흐름 요약

```
notification-service                delivery-service                 analytics-service
       │                                  │                                  │
       │  NotificationEvent               │                                  │
       ├─────────────────────────────────→│                                  │
       │     [notifications 토픽]          │                                  │
       │     key=tenantId                  │                                  │
       │     파티션 3개                     │                                  │
       │                                  │                                  │
       │                          채널 발송 시도                               │
       │                          (스텁: 로그 출력)                            │
       │                                  │                                  │
       │                          실패 시 재시도                               │
       │                          retry-1000 → retry-2000 → DLQ              │
       │                                  │                                  │
       │                                  │  DeliveryResultEvent             │
       │                                  ├─────────────────────────────────→│
       │                                  │     [delivery-results 토픽]      │
       │                                  │     key=tenantId                 │
       │                                  │     파티션 3개                    │
       │                                  │                          MongoDB 저장 +
       │                                  │                          Redis INCR
```

---

## 2. Redis

### 2.1 인프라 구성

- **이미지**: `redis:7.2`
- **포트**: 6379
- **인증**: 없음 (로컬 개발용)
- **볼륨**: `redis_data:/data` (컨테이너 재시작 시 데이터 유지)

### 2.2 용도 1 — 멱등성 체크 (notification-service)

**목적**: 네트워크 타임아웃 등으로 클라이언트가 동일 요청을 재전송할 경우, 중복 알림 발송 방지

**구현 클래스**: `RedisIdempotencyAdapter` → `IdempotencyPort` 구현

**키 패턴**:

```
키:  idempotency:notification:{tenantId}:{idempotencyKey}
값:  "1"
TTL: 24시간 (Duration.ofHours(24))
```

**동작 순서**:

```
POST /api/notifications { ..., idempotencyKey: "order-12345" }
  │
  └─ CreateNotificationService.create()
       │
       │ ① idempotencyPort.isDuplicate(tenantId, idempotencyKey)
       │    └─ Redis: EXISTS idempotency:notification:tenant-001:order-12345
       │
       ├─ 키가 존재하면 (중복 요청):
       │    └─ metrics.incrementDuplicate()
       │    └─ throw BusinessException(DUPLICATE_NOTIFICATION) → 409 Conflict
       │    └─ Kafka 발행하지 않음 (여기서 종료)
       │
       └─ 키가 없으면 (신규 요청):
            │ ② Notification 생성 → MySQL 저장
            │ ③ idempotencyPort.save(tenantId, idempotencyKey)
            │    └─ Redis: SET idempotency:notification:tenant-001:order-12345 "1" EX 86400
            │ ④ Kafka 발행
            └─ return { notificationId, status: "PUBLISHED" }
```

**TTL 24시간의 의미**:

- 24시간 이내 같은 키로 재요청 → 중복으로 판단하여 거부
- 24시간 이후 → Redis에서 키 자동 만료 → 같은 키로 새 요청 가능
- 이 시간은 "클라이언트가 장애 후 재시도하는 최대 허용 시간"이라는 비즈니스 결정

**예시 키**:

```
idempotency:notification:tenant-001:order-12345
idempotency:notification:tenant-002:campaign-2026-03
```

---

### 2.3 용도 2 — 실시간 발송 카운터 (analytics-service)

**목적**: 테넌트별 발송 성공/실패 건수를 실시간으로 조회 가능하도록 Redis INCR로 카운터 관리

**구현 클래스**: `RedisRealtimeCounterAdapter` → `RealtimeCounterPort` 구현

**키 패턴**:

```
채널별 성공:  realtime:{tenantId}:success:{channel}   예) realtime:tenant-001:success:EMAIL
채널별 실패:  realtime:{tenantId}:failure:{channel}   예) realtime:tenant-001:failure:SMS
전체 성공:   realtime:{tenantId}:success:total        예) realtime:tenant-001:success:total
전체 실패:   realtime:{tenantId}:failure:total        예) realtime:tenant-001:failure:total
```

> TTL 없음 — 누적 카운터이므로 만료되지 않습니다.

**쓰기 동작** (Kafka 메시지 수신 시):

```
Kafka delivery-results 수신
  └─ RecordDeliveryEventService.record()
       │
       ├─ status=SUCCESS, channel=EMAIL, tenantId=tenant-001:
       │    └─ INCR realtime:tenant-001:success:EMAIL     (채널별)
       │    └─ INCR realtime:tenant-001:success:total     (전체)
       │
       └─ status=FAILED, channel=SMS, tenantId=tenant-001:
            └─ INCR realtime:tenant-001:failure:SMS       (채널별)
            └─ INCR realtime:tenant-001:failure:total     (전체)
```

> `INCR`은 Redis 원자적(atomic) 연산 — 동시에 여러 메시지가 들어와도 카운터 정합성 보장

**읽기 동작** (API 조회 시):

```
GET /api/analytics/realtime
Header: X-Tenant-Id: tenant-001

  └─ GetRealtimeStatsService.getByTenant("tenant-001")
       │
       │ ① realtimeCounterPort.getTotalSuccess("tenant-001")
       │    └─ Redis: GET realtime:tenant-001:success:total → "150"
       │
       │ ② realtimeCounterPort.getTotalFailed("tenant-001")
       │    └─ Redis: GET realtime:tenant-001:failure:total → "3"
       │
       └─ return RealtimeStats(totalSuccess=150, totalFailed=3, totalSent=153)
```

**직접 확인 명령어**:

```bash
# 전체 성공 건수
docker exec notification-hub-redis redis-cli GET "realtime:tenant-001:success:total"

# 채널별 성공 건수
docker exec notification-hub-redis redis-cli GET "realtime:tenant-001:success:EMAIL"
docker exec notification-hub-redis redis-cli GET "realtime:tenant-001:success:SMS"
docker exec notification-hub-redis redis-cli GET "realtime:tenant-001:success:PUSH"

# 전체 실패 건수
docker exec notification-hub-redis redis-cli GET "realtime:tenant-001:failure:total"

# 테넌트 관련 키 전체 조회
docker exec notification-hub-redis redis-cli KEYS "realtime:tenant-001:*"
```

---

### 2.4 용도 3 — Rate Limiting (api-gateway)

**목적**: IP별 과도한 요청을 차단하여 서비스를 보호

**구현 방식**: Spring Cloud Gateway의 `RequestRateLimiter` 필터 + Redis 백엔드 (토큰 버킷 알고리즘)

**키 기준**: 클라이언트 IP 주소 (`GatewayConfig.ipKeyResolver()` — `exchange.getRequest().getRemoteAddress()`)

**설정** (`api-gateway/application.yml`):

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 100   # 초당 100개 토큰 충전
      redis-rate-limiter.burstCapacity: 200   # 토큰 버킷 최대 용량 200개
      key-resolver: "#{@ipKeyResolver}"       # IP 기준
```

**토큰 버킷 동작 원리**:

```
버킷 (최대 200개 토큰)
  │
  │  매 초 100개씩 토큰 충전 (replenishRate)
  │
  ├─ 요청 도착 → 토큰 1개 차감 → 요청 허용
  ├─ 요청 도착 → 토큰 1개 차감 → 요청 허용
  └─ 요청 도착 → 토큰 0개 → HTTP 429 Too Many Requests
```

- 평상시: 초당 100개 요청까지 안정적으로 처리
- 순간 트래픽 폭증: 최대 200개까지 허용 (버킷에 쌓인 토큰 소진)
- Redis에 토큰 상태를 저장 → api-gateway가 여러 인스턴스로 스케일링되어도 IP당 제한이 정확하게 적용

**적용 범위**: `user-service` 라우트(`/api/users/**`, `/api/keys/**`)에만 Rate Limiter 필터 적용. 다른 서비스 라우트에는 JWT 인증 필터만 적용.

---

## 3. 전체 연동 흐름 요약

```
클라이언트
    │
    ▼
┌──────────────────────────────────────────────────────┐
│ api-gateway (8080)                                   │
│   Redis ① Rate Limiting (IP별 토큰 버킷)              │
│   JWT 토큰 검증                                       │
│   Circuit Breaker (Resilience4j)                     │
└──────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────┐
│ notification-service (8082)                          │
│   Redis ② 멱등성 체크 (idempotency:{tenantId}:{key}) │
│   → 중복이면 409 Conflict                             │
│   → 신규면 MySQL 저장 + Redis 키 저장 (TTL 24h)       │
│   → Kafka 발행                                       │
└──────────────────────────────────────────────────────┘
    │
    │ Kafka: notifications 토픽 (key=tenantId, 파티션 3개)
    ▼
┌──────────────────────────────────────────────────────┐
│ delivery-service (8083)                              │
│   Kafka Consumer (groupId: delivery-service)         │
│   → 채널별 발송 (스텁)                                │
│   → Resilience4j @Retry + @CircuitBreaker            │
│   → 실패 시: retry-1000 → retry-2000 → DLQ           │
│   → 결과 Kafka 발행                                   │
└──────────────────────────────────────────────────────┘
    │
    │ Kafka: delivery-results 토픽 (key=tenantId, 파티션 3개)
    ▼
┌──────────────────────────────────────────────────────┐
│ analytics-service (8084)                             │
│   Kafka Consumer (groupId: analytics-service)        │
│   → MongoDB: delivery_events 컬렉션 (이벤트 원본)     │
│   → MongoDB: daily_stats 컬렉션 (일별 집계)           │
│   → Redis ③ INCR 카운터 (realtime:{tenantId}:...)    │
│                                                      │
│   GET /api/analytics/realtime → Redis 카운터 조회     │
│   GET /api/analytics/daily    → MongoDB 집계 조회     │
└──────────────────────────────────────────────────────┘
```

### Redis 키 전체 요약

| 서비스 | 키 패턴 | 용도 | TTL |
|--------|---------|------|-----|
| api-gateway | Spring 내부 관리 (토큰 버킷) | IP별 요청 제한 | 자동 |
| notification-service | `idempotency:notification:{tenantId}:{key}` | 중복 발송 방지 | 24시간 |
| analytics-service | `realtime:{tenantId}:success:{channel}` | 채널별 성공 카운터 | 없음 (영구) |
| analytics-service | `realtime:{tenantId}:failure:{channel}` | 채널별 실패 카운터 | 없음 (영구) |
| analytics-service | `realtime:{tenantId}:success:total` | 전체 성공 카운터 | 없음 (영구) |
| analytics-service | `realtime:{tenantId}:failure:total` | 전체 실패 카운터 | 없음 (영구) |

### Kafka 토픽 전체 요약

| 토픽 | 파티션 | 키 | 발행 | 소비 | 직렬화 |
|------|--------|-----|------|------|--------|
| `notifications` | 3 | tenantId | notification-service | delivery-service | JSON |
| `notifications-retry-1000` | 자동 | — | @RetryableTopic | delivery-service | JSON |
| `notifications-retry-2000` | 자동 | — | @RetryableTopic | delivery-service | JSON |
| `notifications.dlq` | 1 | — | @RetryableTopic | 수동 확인 | JSON |
| `delivery-results` | 3 | tenantId | delivery-service | analytics-service | JSON |
| `delivery-results.dlq` | 1 | — | kafka-init 생성 | — | — |
