# Notification Hub

B2B SaaS형 멀티테넌트 알림 발송 플랫폼입니다.
고객사(테넌트)가 자사 서비스의 사용자에게 Email/SMS/Push 알림을 API로 요청하면, 플랫폼이 대신 발송하는 구조입니다.

Clean Architecture(Port & Adapter) 기반 마이크로서비스로 설계되어, 도메인 로직이 Spring/JPA에 의존하지 않는 순수 Java로 구현되어 있습니다.

> 현재 채널 발송(Email/SMS/Push)은 스텁(로그 출력)으로 구현되어 있으며, 실제 외부 API(SendGrid, Twilio, FCM) 연동은 포함되어 있지 않습니다.

### 주요 설계 포인트

- **시크릿 외부화**: DB 비밀번호, JWT 시크릿 등 민감 정보를 환경변수(`${DB_PASSWORD}`, `${JWT_SECRET}`)로 분리. `.env` 파일로 관리하며 `.gitignore`에 포함
- **테넌트 격리**: api-gateway에서 JWT 클레임 기반으로 `X-Tenant-Id` 헤더를 주입. 클라이언트가 보낸 헤더는 제거 후 재주입하여 위조 방지
- **Kafka 발행 신뢰성**: fire-and-forget 대신 동기 확인(`.get(5초)`) + 실패 시 예외 전파/로깅
- **멱등성 보장**: notification-service(Redis 키)와 delivery-service(notificationId 중복 체크) 양쪽에서 중복 방지
- **원자적 통계 집계**: MongoDB `$inc` + `upsert`로 DailyStats Race Condition 방지
- **트랜잭션 일관성**: notification-service의 알림 접수 플로우에 `@Transactional` 적용

---

## 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| Language | Java | 17 |
| Framework | Spring Boot, Spring Cloud | 3.2.5, 2023.0.1 |
| RDBMS | MySQL + Spring Data JPA | 8.0 |
| NoSQL | MongoDB | 7.0 |
| Cache | Redis | 7.2 |
| Message Broker | Kafka (KRaft — ZooKeeper 없음) | 3.7 |
| 인증 | Spring Security + JWT (Access 1h / Refresh 24h) | — |
| 장애 대응 | Resilience4j (Circuit Breaker + Retry) | — |
| 모니터링 | Prometheus + Grafana + Micrometer + Zipkin | — |
| 컨테이너 | Docker Compose / Kubernetes | — |
| IaC | Terraform (local Docker + AWS 설계) | — |
| CI/CD | GitHub Actions (test + Docker 이미지 빌드) | — |

---

## 아키텍처

### Clean Architecture 의존성 방향

```
presentation/    → domain/port/in 호출 (REST Controller → UseCase)
application/     → domain/port/in 구현 + domain/port/out 호출
infrastructure/  → domain/port/out 구현 (JPA, Kafka, Redis 등)
domain/          → 아무것도 의존하지 않음 (순수 Java)
```

핵심 원칙: **안쪽 레이어(domain)는 바깥(infrastructure)을 모른다.** 인프라 교체(예: MySQL → PostgreSQL) 시 도메인 코드 변경 없이 어댑터만 교체하면 됩니다.

### 패키지 구조 (서비스 공통)

```
{service}/src/main/java/com/notificationhub/{service}/
├── domain/
│   ├── model/           ← 순수 도메인 엔티티 & VO (Spring/JPA 의존 없음)
│   ├── exception/       ← 도메인 예외
│   └── port/
│       ├── in/          ← UseCase 인터페이스 (외부 → 도메인)
│       └── out/         ← 인프라 인터페이스 (도메인 → 외부)
├── application/
│   └── service/         ← UseCase 구현체 (포트만 의존)
├── infrastructure/
│   ├── persistence/     ← JPA/Mongo @Entity + Repository + Mapper + Adapter
│   ├── messaging/       ← Kafka Producer/Consumer + Config
│   ├── cache/           ← Redis 어댑터
│   └── external/        ← 외부 API 클라이언트 (Resilience4j)
└── presentation/
    ├── controller/      ← REST Controller (UseCase 호출)
    └── dto/             ← Request/Response DTO (Java record)
```

---

## 서비스 구성

| 서비스 | 포트 | 역할 | 주요 의존 |
|--------|------|------|----------|
| **discovery-service** | 8761 | Eureka 서비스 레지스트리 | — |
| **api-gateway** | 8080 | 라우팅 + JWT 인증 필터 + 테넌트 헤더 주입 + Rate Limiting + Circuit Breaker | Redis |
| **user-service** | 8081 | 테넌트/사용자 관리 + JWT 발급 + API Key 관리 | MySQL |
| **notification-service** | 8082 | 알림 접수 + 멱등성 체크 + Kafka 발행 | MySQL, Redis, Kafka |
| **delivery-service** | 8083 | Kafka 소비 + 채널별 발송 + 재시도/DLQ + 결과 발행 | MySQL, Kafka |
| **analytics-service** | 8084 | 발송 결과 집계 (이벤트 저장 + 일별 통계 + 실시간 카운터) | MongoDB, Redis, Kafka |

---

## 전체 서비스 플로우

```
클라이언트 (고객사)
   │
   │  POST /api/users/register → JWT 발급
   │  POST /api/notifications  → 알림 발송 요청
   ▼
┌─────────────────────────────────────────────────────────┐
│ api-gateway (8080)                                      │
│  ├─ Redis Rate Limiter (IP별 초당 100건, 버스트 200건)    │
│  ├─ JWT 토큰 검증 → X-Tenant-Id/X-User-Id 헤더 주입      │
│  └─ Circuit Breaker (Resilience4j — 하위 서비스 장애 차단) │
└─────────────────────────────────────────────────────────┘
   │                                      │
   ▼                                      ▼
user-service (8081)             notification-service (8082)
 · 테넌트 등록/로그인              · 멱등성 체크 (Redis)
 · JWT 발급                       · 알림 생성 → MySQL 저장
 · API Key 관리                   · Kafka 발행
                                         │
                              ┌──────────┘
                              │  [Kafka: notifications 토픽]
                              │  key=tenantId, 파티션 3개
                              ▼
                     delivery-service (8083)
                      · Kafka 소비 → 채널별 발송 (스텁)
                      · @CircuitBreaker + @Retry
                      · 실패 시 재시도: 1s → 2s → DLQ
                              │
                    ┌─────────┴─────────┐
                    │                   │
               성공/실패           3회 모두 실패
                    │                   │
        [delivery-results 토픽]   [notifications.dlq]
                    │             (DlqConsumer 로깅)
                    ▼
           analytics-service (8084)
            · MongoDB: 이벤트 원본 + 일별 집계
            · Redis INCR: 실시간 카운터
                    │
         ┌──────────┴──────────┐
         │                     │
   GET /analytics/realtime   GET /analytics/daily
   (Redis 카운터 조회)        (MongoDB 집계 조회)
```

---

## 단계별 동작 상세

### 1단계 — 테넌트 등록 (user-service)

고객사가 플랫폼에 가입하여 JWT 토큰을 발급받는 단계입니다.

**API:**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/users/register` | 테넌트 + 관리자 계정 동시 생성, JWT 반환 |
| POST | `/api/users/login` | JWT 재발급 |
| POST | `/api/keys` | API Key 생성 (테넌트별 여러 개 가능) |

**`POST /api/users/register` 처리 흐름:**

```
요청: { name, email, password }
  │
  ├─ ① 이메일 중복 검사 → 중복 시 409 Conflict
  ├─ ② Tenant 도메인 객체 생성 (plan: FREE)
  ├─ ③ User 도메인 객체 생성 (role: ADMIN, BCrypt 패스워드 해시)
  ├─ ④ MySQL 저장 (JPA 어댑터)
  └─ ⑤ JWT 발급 후 반환
       ├─ Access Token  (유효기간: 1시간)
       └─ Refresh Token (유효기간: 24시간)
```

**도메인 모델:**

| 모델 | 설명 |
|------|------|
| `Tenant` | 고객사 (id, name, email, plan, active) |
| `User` | 관리자 계정 (id, tenantId, email, encodedPassword, role) — 이메일은 tenantId + email 복합 유니크 |
| `ApiKey` | API 키 (id, tenantId, name, keyValue=`nhub_xxx`, expiresAt, revoked) |
| `SubscriptionPlan` | 요금제 VO (FREE, BASIC, PREMIUM, ENTERPRISE) |

> Tenant : User = 1 : 1 관계. 회원가입 시 Tenant와 User가 동시에 생성됩니다.

---

### 2단계 — 알림 접수 (notification-service)

고객사가 자사 사용자에게 보낼 알림을 요청하는 단계입니다.

**API:**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/notifications` | 알림 접수 (멱등성 체크 → Kafka 발행) |
| GET | `/api/notifications/{id}` | 알림 상세 조회 |

**`POST /api/notifications` 처리 흐름:**

> 전체 플로우는 `@Transactional`로 감싸져 있어, 중간 실패 시 DB 변경이 롤백됩니다.

```
요청: { channel, recipient, content, idempotencyKey }
헤더: X-Tenant-Id: {tenantId} (api-gateway가 JWT에서 추출하여 주입)
  │
  ├─ ① Redis 멱등성 체크
  │    키: idempotency:notification:{tenantId}:{idempotencyKey}
  │    ├─ 키 존재 → 409 Conflict (중복 발송 방지, Kafka 발행 안 함)
  │    └─ 키 없음 → 계속 진행
  │
  ├─ ② Notification 도메인 객체 생성 (상태: PENDING)
  ├─ ③ notification.publish() → 상태를 PUBLISHED로 전이
  ├─ ④ MySQL 저장 (JPA 어댑터)
  ├─ ⑤ Redis에 멱등성 키 저장 (TTL: 24시간)
  ├─ ⑥ Kafka notifications 토픽에 NotificationEvent 발행 (동기 확인, 5초 타임아웃)
  ├─ ⑦ Prometheus 메트릭 증가 (notification.sent.total)
  └─ 응답: { notificationId, status: "PUBLISHED" }
```

**멱등성 키 동작 원리:**

클라이언트가 네트워크 타임아웃 등으로 같은 요청을 재전송할 경우, `idempotencyKey`가 동일하면 중복으로 판단하여 409를 반환합니다. 24시간 후 Redis에서 키가 만료되면 같은 키로 새 요청이 가능합니다.

---

### 3단계 — 채널 발송 (delivery-service)

Kafka에서 알림 이벤트를 소비하여 실제 채널로 발송하는 단계입니다.

**API:**

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/deliveries/{id}` | 발송 로그 상세 조회 |
| GET | `/api/deliveries` | 발송 로그 목록 조회 |

**Kafka 소비 → 발송 처리 흐름:**

```
Kafka: notifications 토픽 수신
  │
  ├─ ⓪ 멱등성 체크: notificationId로 기존 DeliveryLog 존재 여부 확인
  │    └─ 이미 존재 → 기존 결과 반환 (중복 처리 방지)
  │
  ├─ ① DeliveryLog 도메인 객체 생성 (상태: PENDING) → MySQL 저장
  │
  ├─ ② ChannelDelivererAdapter.deliver(channel, recipient, content)
  │    ├─ @CircuitBreaker: 최근 10회 중 50% 실패 → OPEN (10초간 전체 차단)
  │    ├─ @Retry: 최대 3회, 지수 백오프 (1s → 2s → 4s)
  │    └─ channel 분기: sendEmail() / sendSms() / sendPush() (현재 스텁)
  │
  ├─ 발송 성공:
  │    ├─ ③ log.markSuccess() → MySQL 상태 업데이트 (SUCCESS)
  │    └─ ④ Kafka delivery-results 토픽에 DeliveryResultEvent.success() 발행
  │
  └─ 발송 실패:
       ├─ ③ log.markFailed(reason) → MySQL 상태 업데이트 (FAILED)
       └─ ④ Kafka delivery-results 토픽에 DeliveryResultEvent.failure() 발행
```

**재시도 + DLQ 동작 (2단계 방어):**

```
[Resilience4j @Retry — 어댑터 내부 즉시 재시도]
  └─ 1s → 2s → 4s 간격으로 3회 시도
       └─ 그래도 실패 시 예외가 위로 전파

[@RetryableTopic — Kafka 토픽 기반 비차단 재시도]
  └─ 1회 실패 → notifications-retry-1000 토픽 (1초 후 재소비)
  └─ 2회 실패 → notifications-retry-2000 토픽 (2초 후 재소비)
  └─ 3회 실패 → notifications.dlq 토픽 (DlqConsumer가 로깅, 수동 확인 필요)
```

**Circuit Breaker 상태 전이:**

```
CLOSED (정상) ──실패율 50% 초과──→ OPEN (차단: fallback에서 예외 발생 → FAILED 처리)
                                    │
                                  10초 후
                                    │
                                    ▼
                               HALF_OPEN (탐색)
                                 ├─ 성공 → CLOSED
                                 └─ 실패 → OPEN
```

---

### 4단계 — 통계 집계 (analytics-service)

발송 결과를 MongoDB에 저장하고, Redis 실시간 카운터를 갱신하는 단계입니다.

**API:**

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/analytics/realtime` | 실시간 통계 (Redis 카운터 조회) |
| GET | `/api/analytics/daily?date=YYYY-MM-DD` | 일별 통계 (MongoDB 집계 조회) |

**Kafka 소비 → 집계 처리 흐름:**

```
Kafka: delivery-results 토픽 수신
  │
  ├─ ① DeliveryEvent 도메인 객체 생성 → MongoDB delivery_events 컬렉션 저장
  │
  ├─ ② DailyStats 원자적 업데이트 (MongoDB $inc + upsert)
  │    └─ 테넌트 + 날짜 기준으로 문서가 없으면 생성, 있으면 카운터만 증가
  │       (read-modify-write 대신 단일 원자적 연산으로 Race Condition 방지)
  │
  ├─ ③ 성공/실패 분기:
  │    ├─ SUCCESS → $inc(totalSent, totalSuccess, channelCounts) + Redis INCR 성공 카운터
  │    └─ FAILED  → $inc(totalSent, totalFailed, channelCounts) + Redis INCR 실패 카운터
  │
  └─ ④ 완료 (별도 save 호출 없이 upsert로 처리 완료)
```

**MongoDB 컬렉션:**

| 컬렉션 | 용도 | 주요 필드 |
|--------|------|----------|
| `delivery_events` | 발송 이벤트 원본 로그 | deliveryLogId, notificationId, tenantId, channel, status, failureReason, occurredAt |
| `daily_stats` | 테넌트별 일간 집계 | tenantId, date, totalSent, totalSuccess, totalFailed, channelCounts (Map) |

**Redis 실시간 카운터:**

| 키 패턴 | 용도 |
|---------|------|
| `realtime:{tenantId}:success:{channel}` | 채널별 성공 건수 |
| `realtime:{tenantId}:failure:{channel}` | 채널별 실패 건수 |
| `realtime:{tenantId}:success:total` | 전체 성공 건수 |
| `realtime:{tenantId}:failure:total` | 전체 실패 건수 |

> `INCR` 명령어는 Redis 원자적(atomic) 연산으로, 동시에 여러 메시지가 들어와도 카운터 정합성이 보장됩니다. TTL 없이 영구 누적됩니다.

---

## 모니터링

### Prometheus + Grafana

- 전 서비스 `/actuator/prometheus` 엔드포인트 노출 (Micrometer)
- Prometheus가 주기적으로 수집 → Grafana에서 시각화

**커스텀 메트릭 (notification-service):**

| 메트릭 | 설명 |
|--------|------|
| `notification.sent.total` | 발송된 알림 건수 |
| `notification.duplicate.total` | 멱등성 키 중복으로 거부된 건수 |

**Grafana 대시보드:** `http://localhost:3000` (admin / admin1234)

| 패널 | 내용 |
|------|------|
| Notifications Sent | 발송 건수 추이 |
| Notifications Duplicate | 중복 거부 건수 추이 |
| HTTP Request Rate | 서비스별 초당 요청 수 |
| HTTP 5xx Error Rate | 서비스별 서버 에러율 |
| JVM Heap Usage | 서비스별 힙 메모리 사용량 |
| Kafka Consumer Lag | 토픽별 소비 지연 |

### Zipkin 분산 트레이싱

- `http://localhost:9411`
- notification-service → Kafka → delivery-service 구간의 요청 흐름을 트레이스로 확인 가능

---

## 인프라 구성

### Docker Compose 서비스

| 서비스 | 포트 | 용도 |
|--------|------|------|
| MySQL | 3307 (→3306) | user / notification / delivery 데이터 저장 |
| MongoDB | 27017 | analytics 이벤트/통계 저장 |
| Redis | 6379 | 멱등성 키 + Rate Limiter + 실시간 카운터 |
| Kafka | 9092 | 서비스 간 이벤트 브로커 (KRaft 모드, 단일 노드) |
| Prometheus | 9090 | 메트릭 수집 |
| Grafana | 3000 | 메트릭 시각화 |
| Zipkin | 9411 | 분산 트레이싱 |

### Kafka 토픽

| 토픽 | 파티션 | 발행 서비스 | 소비 서비스 | 용도 |
|------|--------|-----------|-----------|------|
| `notifications` | 3 | notification-service | delivery-service | 알림 발송 요청 |
| `notifications-retry-1000` | 자동 | delivery-service | delivery-service | 1차 재시도 (1초 지연) |
| `notifications-retry-2000` | 자동 | delivery-service | delivery-service | 2차 재시도 (2초 지연) |
| `notifications.dlq` | 1 | delivery-service | delivery-service (DlqConsumer) | 최종 실패 메시지 로깅 |
| `delivery-results` | 3 | delivery-service | analytics-service | 발송 결과 집계 |

### Kubernetes

`k8s/` 디렉토리에 Plain YAML 매니페스트로 구성되어 있습니다.

| 리소스 | 대상 |
|--------|------|
| Namespace | `notification-hub` |
| ConfigMap / Secret | 공통 설정, DB/Kafka 접속 정보 |
| Deployment + Service | 전 6개 서비스 |
| Ingress | api-gateway (nginx, `notification-hub.local`) |
| HPA | user-service, notification-service (CPU 70% 기준, min 2 / max 10 Pod) |

> HPA는 트래픽 급증 시 자동으로 Pod를 2~10개 범위에서 스케일링합니다. delivery-service와 analytics-service는 Kafka Consumer이므로 파티션 수(3개)가 병렬 처리 상한을 결정합니다.

### Terraform

| 환경 | 경로 | 구성 |
|------|------|------|
| Local | `terraform/local/` | Docker provider로 MySQL/Redis/MongoDB/Kafka 컨테이너 관리 |
| AWS (설계) | `terraform/aws/` | VPC, EKS, RDS(MySQL), MSK(Kafka), ElastiCache(Redis), DocumentDB 모듈 |

> AWS 구성은 설계 코드만 포함되어 있으며, 실제 `terraform apply`는 하지 않습니다.

### CI/CD

`.github/workflows/ci.yml` — 전체 테스트 실행 + 7개 서비스 Docker 이미지 빌드 (matrix strategy)

---

## 서비스 실행 방법

### 1단계 — 인프라 기동 (Docker)

```bash
cd notification-hub
docker compose up -d
```

컨테이너 상태 확인:

```bash
docker compose ps
```

### 2단계 — Discovery Service

```bash
mvn spring-boot:run -pl discovery-service
```

Eureka 대시보드: http://localhost:8761

### 3단계 — API Gateway

```bash
mvn spring-boot:run -pl api-gateway
```

### 4단계 — 비즈니스 서비스 (각각 별도 터미널, 순서 무관)

```bash
mvn spring-boot:run -pl user-service
mvn spring-boot:run -pl notification-service
mvn spring-boot:run -pl delivery-service
mvn spring-boot:run -pl analytics-service
```

### 실행 순서 요약

```
Docker 인프라 → discovery-service → api-gateway → 비즈니스 서비스 (병렬)
```

### 종료

```bash
# 서비스: 각 터미널에서 Ctrl+C
# 인프라 (볼륨 유지):
docker compose stop

# 인프라 (볼륨 삭제):
docker compose down -v
```

---

## 테스트

### 단위 테스트 커버리지

| 서비스 | 테스트 수 | 커버리지 (domain + application) |
|--------|-----------|--------------------------------|
| user-service | 20/20 | 90.0% |
| notification-service | 12/12 | 83.5% |
| delivery-service | 12/12 | 93.4% |
| analytics-service | 17/17 | 94.7% |

```bash
# 전체 테스트 실행
mvn test

# 서비스별 커버리지 리포트
mvn test jacoco:report -pl user-service
# 브라우저에서 target/site/jacoco/index.html 확인
```

### E2E 플로우 테스트

**1. 테넌트 등록 → JWT 발급**

```bash
curl -s -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"test-tenant","email":"admin@test.com","password":"pass1234"}'
# → 응답에서 accessToken 추출
```

**2. 알림 발송**

```bash
curl -s -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT}" \
  -d '{"channel":"EMAIL","recipient":"user@example.com","content":"Hello","idempotencyKey":"key-001"}'
# → { notificationId, status: "PUBLISHED" }
```

**3. 중복 발송 테스트 (같은 idempotencyKey)**

```bash
# 위와 동일한 요청 재전송
# → 409 Conflict (중복 거부)
```

**4. 실시간 통계 확인**

```bash
curl -s http://localhost:8080/api/analytics/realtime \
  -H "Authorization: Bearer {JWT}"
# → { totalSuccess, totalFailed, totalSent }
```

**5. 일별 통계 확인**

```bash
curl -s "http://localhost:8080/api/analytics/daily?date=$(date +%Y-%m-%d)" \
  -H "Authorization: Bearer {JWT}"
# → 채널별 성공/실패 카운트
```

**6. 인프라 직접 확인**

```bash
# Redis 실시간 카운터
docker exec notification-hub-redis redis-cli KEYS "realtime:*"

# MongoDB 발송 이벤트
docker exec notification-hub-mongodb mongosh \
  -u nhub -p nhub1234 --authenticationDatabase admin \
  --eval "db.getSiblingDB('analytics').delivery_events.find().sort({occurredAt:-1}).limit(5).pretty()"

# Kafka 토픽 메시지 확인
docker exec notification-hub-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic delivery-results --from-beginning --max-messages 5
```

---

## 프로젝트 구조

```
notification-hub/
├── common/                  ← 공유 모듈 (이벤트 record, 예외, JWT 유틸)
├── discovery-service/       ← Eureka Server
├── api-gateway/             ← Spring Cloud Gateway
├── user-service/            ← 테넌트/사용자/API Key 관리
├── notification-service/    ← 알림 접수 + 멱등성 + Kafka 발행
├── delivery-service/        ← Kafka 소비 + 채널 발송 + 재시도/DLQ
├── analytics-service/       ← 통계 집계 (MongoDB + Redis)
├── docker-compose.yml       ← 인프라 컨테이너 (7개)
├── docker/mysql/init.sql    ← MySQL 초기화 스크립트
├── monitoring/
│   ├── prometheus/          ← prometheus.yml
│   └── grafana/             ← 대시보드 JSON + 데이터소스 설정
├── k8s/                     ← Kubernetes 매니페스트
│   ├── namespace.yaml, configmap.yaml, secret.yaml
│   └── {service}/deployment.yaml, service.yaml, [hpa.yaml, ingress.yaml]
├── terraform/
│   ├── local/               ← Docker provider (로컬 인프라)
│   └── aws/                 ← VPC, EKS, RDS, MSK, ElastiCache, DocumentDB (설계)
├── .github/workflows/ci.yml ← GitHub Actions CI/CD
└── docs/
    ├── kafka-redis.md       ← Kafka & Redis 동작 상세 문서
    └── improvement-todo.md  ← 코드 리뷰 기반 개선 사항 (P0/P1 완료, P2 미착수)
```
