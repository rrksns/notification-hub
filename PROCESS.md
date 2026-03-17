# Notification Hub — 개발 프로세스 기록

## 프로젝트 개요

**목표**: 전통적 레이어드 아키텍처를 **Clean Architecture(Port & Adapter)**로 재설계한 멀티테넌트 알림 허브
**목적**: 포트폴리오 — Clean Architecture + TDD + Observability + K8s + IaC 조합으로 시니어급 설계 역량 어필
**저장소**: https://github.com/rrksns/notification-hub

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5, Spring Cloud 2023.0.1 |
| Build | Maven Multi-Module |
| RDBMS | MySQL 8.0 + Spring Data JPA |
| NoSQL | MongoDB 7.0 |
| Cache | Redis 7.2 |
| Message Broker | Kafka 3.7 (KRaft, ZooKeeper 없음) |
| Auth | Spring Security + JWT (JJWT 0.12.5) |
| Circuit Breaker | Resilience4j |
| Monitoring | Prometheus + Grafana + Micrometer |
| Container | Docker + Kubernetes |
| IaC | Terraform (local Docker + AWS 설계) |
| CI/CD | GitHub Actions |
| Test | JUnit 5 + Mockito + JaCoCo |

---

## Clean Architecture 의존성 규칙

```
domain/      → 아무것도 의존하지 않음 (순수 Java)
application/ → domain/port/ 만 의존
infrastructure/ → domain/port/out/ 구현
presentation/   → domain/port/in/ 호출
```

### 서비스별 패키지 구조 (공통)

```
{service}/src/main/java/com/notificationhub/{service}/
├── domain/
│   ├── model/       ← 순수 도메인 엔티티 & VO (Spring/JPA 의존 없음)
│   ├── exception/   ← 도메인 예외
│   └── port/
│       ├── in/      ← UseCase 인터페이스
│       └── out/     ← 인프라 인터페이스
├── application/
│   └── service/     ← UseCase 구현체 (포트만 의존)
├── infrastructure/
│   ├── persistence/ ← JPA/MongoDB 어댑터
│   ├── messaging/   ← Kafka Producer/Consumer
│   ├── cache/       ← Redis 어댑터
│   └── security/    ← JWT, BCrypt 어댑터
└── presentation/
    ├── controller/  ← REST Controller
    └── dto/         ← Request/Response records
```

---

## Phase별 개발 과정

### Phase 1: Project Skeleton + Common Module
**커밋**: `feat: Phase 1+2`

**작업 내용**:
- Root `pom.xml` — Spring Boot 3.2.5 BOM, Spring Cloud 2023.0.1 BOM, JaCoCo 설정
- `common` 모듈 — `ApiResponse<T>`, `PageResponse<T>`, `BusinessException`, `ErrorCode`, `GlobalExceptionHandler`, `NotificationEvent`, `DeliveryResultEvent`, `JwtProperties`, `JwtTokenProvider`
- `discovery-service` — Eureka Server (`@EnableEurekaServer`)
- `api-gateway` — Spring Cloud Gateway + JWT 필터 + Redis Rate Limiter + Resilience4j Circuit Breaker
- `docker-compose.yml` — MySQL(3307), MongoDB, Redis, Kafka(KRaft), Prometheus, Grafana, Zipkin
- 스켈레톤 모듈 — user/notification/delivery/analytics-service `pom.xml`

**트러블슈팅**:
- MySQL 포트 3306 → **3307** (OrbStack이 3306 점유)
- analytics-service Kotlin → **Java**로 전환 (학습 비용 절감)
- `version: '3.8'` 제거 (docker-compose obsolete 경고)

**결과**: `mvn clean package -DskipTests` BUILD SUCCESS (8 modules)

---

### Phase 2: user-service (TDD)
**커밋**: `feat: Phase 1+2`
**테스트**: 17/17 통과

**TDD 흐름**:
1. **RED** — `TenantTest`, `UserTest`, `ApiKeyTest`, `RegisterTenantUseCaseTest`, `AuthenticateUseCaseTest` 작성
2. **GREEN** — 도메인 모델 구현 (순수 Java, Spring/JPA 어노테이션 없음)
3. 포트 인터페이스 → 애플리케이션 서비스 → 인프라 어댑터 순으로 구현

**주요 설계 포인트**:
- `Tenant.create()` / `Tenant.reconstruct()` 팩토리 패턴으로 생성 vs 재구성 분리
- `PasswordEncoder`, `TokenProvider` Port Out으로 추상화 → UseCase 테스트 시 Mock만 사용
- JPA `@Entity`와 도메인 `model` 완전 분리 — `TenantEntity.from()` / `.toDomain()` 매핑

**API**:
- `POST /api/users/register` → 테넌트 + 관리자 유저 생성
- `POST /api/auth/login` → JWT 발급
- `POST /api/keys` → API Key 생성

---

### Phase 3: notification-service (TDD)
**커밋**: `feat: Phase 3`
**테스트**: 12/12 통과

**TDD 흐름**:
1. **RED** — `NotificationTest`(상태전이), `ChannelTest`(VO 검증), `CreateNotificationServiceTest`(멱등성) 작성
2. **GREEN** — `Notification.publish()` PENDING→PUBLISHED 상태전이, `Channel.from()` 팩토리

**주요 설계 포인트**:
- **멱등성**: `IdempotencyPort` — Redis `SETNX` + 24h TTL
- **상태전이**: PENDING → PUBLISHED, 이미 PUBLISHED면 `InvalidNotificationException`
- **Kafka 발행**: `NotificationEventPublisher` Port Out → `KafkaNotificationEventPublisher` 어댑터

**API**:
- `POST /api/notifications` → 멱등성 체크 → Kafka 발행
- `GET /api/notifications/{id}` → 알림 조회

---

### Phase 4: delivery-service (TDD)
**커밋**: `feat: Phase 4` (별도 Claude 세션에서 구현)
**테스트**: 12/12 통과

**주요 설계 포인트**:
- `DeliveryLog` 상태전이: PENDING → SUCCESS / FAILED
- **Kafka Consumer**: retry 3회 + 지수 백오프(1000ms, 2x) + DLQ(`notifications.dlq`)
- **채널 발송 스텁**: `ChannelDelivererAdapter` — EMAIL/SMS/PUSH (TODO: SendGrid, Twilio, FCM 연동)
- 발송 결과 → `delivery-results` Kafka 토픽 발행

**API**:
- `GET /api/deliveries/{id}` → 배송 로그 조회
- `GET /api/deliveries` → 테넌트별 전체 조회

---

### Phase 5: analytics-service (TDD)
**커밋**: `feat: Phase 5`
**테스트**: 8/8 통과

**주요 설계 포인트**:
- **MongoDB**: `DeliveryEvent`(raw 이벤트 저장), `DailyStats`(일별 집계) — Spring Data MongoDB
- **Redis INCR**: `RealtimeCounterPort` — 채널별 + 테넌트 전체 실시간 카운터
- `DailyStats.recordSuccess()` / `recordFailure()` — 채널별 집계 누적
- `ChannelStats` — 불변 record (`successCount`, `failureCount`, `total()`)

**API**:
- `GET /api/analytics/daily?date=2026-03-17` → 일별 통계
- `GET /api/analytics/realtime` → Redis 실시간 카운터

---

### Phase 6: Observability + K8s + IaC + CI/CD
**커밋**: `feat: Phase 6` + `fix: CI workflow`

**Task 6.1 — Micrometer**:
- 전 서비스 `micrometer-registry-prometheus` 추가
- notification-service 커스텀 메트릭: `notification.sent.total`, `notification.duplicate.total`

**Task 6.2 — Grafana 대시보드** (`monitoring/grafana/dashboards/notification-hub.json`):
- Notifications Sent/Duplicate (stat panel)
- HTTP Request Rate / 5xx Error Rate (timeseries)
- JVM Heap Used (timeseries)
- Kafka Consumer Lag (timeseries)

**Task 6.3 — Kubernetes** (`k8s/`):
- `namespace.yaml`, `configmap.yaml`, `secret.yaml`
- 전 서비스 `Deployment` + `Service`
- `api-gateway` Ingress (nginx, `notification-hub.local`)
- HPA: user/notification-service — CPU 70%, min 2, max 10
- Liveness: `/actuator/health/liveness`, Readiness: `/actuator/health/readiness`
- resources requests/limits 설정

**Task 6.4 — Terraform local** (`terraform/local/`):
- Docker provider — MySQL, Redis, MongoDB, Kafka 컨테이너 관리
- `terraform apply`로 로컬 인프라 프로비저닝 가능

**Task 6.5 — Terraform AWS** (`terraform/aws/`):
- S3 backend + VPC + EKS + RDS(MySQL) + MSK(Kafka 3.5.1) + ElastiCache(Redis) + DocumentDB 모듈
- 설계 어필용 (실제 apply 불필요)

**Task 6.6 — GitHub Actions** (`.github/workflows/ci.yml`):
- `Build & Test` job: `mvn clean verify` → Publish Test Results → Upload JaCoCo Reports
- `Build Docker Images` job: 6개 서비스 matrix 병렬 Docker 빌드
- 전 서비스 `Dockerfile` (eclipse-temurin:17-jre-alpine)

**트러블슈팅**:
- `Publish Test Results` 실패 → `permissions: checks: write` 추가로 해결
- `common` 모듈 Dockerfile 없음 → Docker matrix에서 제거

**결과**: ✅ CI/CD 파이프라인 통과

---

## 전체 테스트 현황

| 서비스 | 테스트 수 | 커버 대상 |
|--------|-----------|-----------|
| user-service | 17 | domain(12) + application(5) |
| notification-service | 12 | domain(9) + application(3) |
| delivery-service | 12 | domain(8) + application(4) |
| analytics-service | 8 | domain(5) + application(3) |
| **합계** | **49** | domain + application 레이어 |

---

## 서비스 포트 구성

| 서비스 | 포트 |
|--------|------|
| api-gateway | 8080 |
| discovery-service (Eureka) | 8761 |
| user-service | 8081 |
| notification-service | 8082 |
| delivery-service | 8083 |
| analytics-service | 8084 |
| MySQL | 3307 (로컬) / 3306 (K8s) |
| MongoDB | 27017 |
| Redis | 6379 |
| Kafka | 9092 |
| Prometheus | 9090 |
| Grafana | 3000 |
| Zipkin | 9411 |

---

## 로컬 실행 방법

```bash
# 1. 인프라 기동
docker-compose up -d

# 2. 전체 빌드
mvn clean package -DskipTests

# 3. 서비스 순서대로 실행
# discovery-service → api-gateway → user/notification/delivery/analytics-service

# 4. Kafka 토픽 확인
docker exec notification-hub-kafka \
  /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# 5. E2E 테스트
# 테넌트 등록
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"TestCorp","email":"admin@test.com","password":"pass1234"}'

# 알림 발송
curl -X POST http://localhost:8080/api/notifications \
  -H "Authorization: Bearer {JWT}" \
  -H "Content-Type: application/json" \
  -d '{"channel":"EMAIL","recipient":"user@test.com","content":"Hello","idempotencyKey":"key-001"}'

# 실시간 통계
curl http://localhost:8080/api/analytics/realtime \
  -H "X-Tenant-Id: {tenantId}"
```

---

## 핵심 학습 포인트

1. **도메인 순수성**: JPA `@Entity`와 도메인 `model`을 분리하면 단위 테스트가 Spring 컨텍스트 없이 가능
2. **Port & Adapter**: `PasswordEncoder`, `TokenProvider` 등을 인터페이스로 추상화하면 UseCase 테스트에서 Mock만으로 충분
3. **팩토리 패턴**: `create()` (새 생성) vs `reconstruct()` (DB 재구성) 분리로 불변 도메인 모델 유지
4. **Kafka KRaft**: ZooKeeper 없이 동작 — `KAFKA_PROCESS_ROLES=broker,controller`로 단일 노드 구성
5. **멱등성**: Redis `SETNX` + TTL 패턴으로 중복 요청 방지
6. **TDD 순서**: RED(실패 테스트) → GREEN(최소 구현) → REFACTOR 순서가 설계 품질을 높임

---

## Git 커밋 히스토리

```
def9586  fix: CI workflow — add permissions for checks:write
fd19dfe  feat: Phase 6 — Observability, K8s, Terraform IaC, GitHub Actions CI/CD
ad5ac22  feat: Phase 5 — analytics-service (Clean Architecture, TDD)
9b3d883  feat: Phase 4 — delivery-service (Clean Architecture, TDD)
6e2459c  feat: Phase 3 — notification-service (Clean Architecture, TDD)
317c4cb  feat: Phase 1+2 — project skeleton, common module, user-service
```
