# Plan: security-hardening

## Executive Summary

| 항목 | 내용 |
|------|------|
| Feature | security-hardening |
| 작성일 | 2026-04-03 |
| 기준 | 코드 리뷰 결과 (P2 미착수 항목 + 신규 발견 항목) |
| 선행 완료 | P0 (보안 긴급) 5건 ✅ / P1 (데이터 무결성) 7건 ✅ |

### Value Delivered (4-Perspective)

| 관점 | 내용 |
|------|------|
| **Problem** | P0/P1 완료 후에도 서비스 직접 호출 시 인증 우회, DB 인덱스 누락, Kafka 신뢰성 설정 미흡, 입력 검증 불완전 등 실무 수준에서 지적받을 수 있는 P2 이슈 27건이 미착수 상태 |
| **Solution** | 포트폴리오 면접 대응에 직결되는 10개 핵심 항목을 선별해 실제 코드로 반영 — 나머지는 개선 방향 문서화로 대체 |
| **Function / UX Effect** | 보안 심층 방어(SecurityConfig JWT 필터), DB 쿼리 성능 개선(인덱스), Kafka 메시지 신뢰성(acks=all), API 입력 검증 강화(@Size 등)가 코드에 반영되어 면접관이 코드를 직접 보더라도 실무 수준으로 납득 가능 |
| **Core Value** | "문제를 인지하고 실제로 고쳤다" — 면접 시 P2 항목을 자신 있게 설명 + 코드로 증명할 수 있는 포트폴리오 완성도 확보 |

---

## 1. 배경 및 목표

### 배경

`improvement-todo.md` 기준 P0(5건)/P1(7건)은 2026-03-24 완료됐으나, P2(15건)는 미착수 상태. 추가 코드 리뷰에서 P2에 포함되지 않은 신규 이슈 12건이 추가 발견됐다.

이 Plan은 **면접에서 실제 코드로 보여줄 수 있는 개선 항목**을 우선 구현하고, 나머지는 문서화로 커버하는 전략을 따른다.

### 목표

- 구현 대상(10건): 코드가 바뀌는 항목, 면접관이 직접 볼 가능성 높은 것들
- 문서화 대상(나머지): "인지하고 있으며 개선 방향을 알고 있다"고 답할 수 있는 수준

---

## 2. 이슈 전체 목록 및 우선순위

### 2.1 구현 대상 (이번 Plan 범위)

| # | 서비스 | 이슈 | 근거 | 구현 난이도 |
|---|--------|------|------|------------|
| A1 | 전체 (JPA) | DB 인덱스 누락 (`@Index`) | 성능 직결, 코드 1줄 | 낮음 |
| A2 | delivery | `ProcessDeliveryService` `@Transactional` 누락 | 데이터 일관성, 면접 빈출 | 낮음 |
| A3 | user | `TenantService.register()` `@Transactional` 누락 | 고아 데이터 방지 | 낮음 |
| A4 | api-gateway | `JwtAuthenticationFilter` 예외처리 (500 → 401) | 보안 + UX | 낮음 |
| A5 | notification | `content` `@Size(max=2000)` 누락 | 입력 검증 강화 | 낮음 |
| A6 | user | 비밀번호 복잡도 검증 (`@Pattern`) | 보안, 면접 빈출 | 낮음 |
| A7 | analytics | `Long.parseLong()` 예외 미처리 | 방어적 프로그래밍 | 낮음 |
| A8 | analytics | `ZoneId.systemDefault()` → `ZoneOffset.UTC` | 컨테이너 타임존 보장 | 낮음 |
| A9 | notification / delivery | Kafka Producer `acks=all`, `enable.idempotence=true` 미설정 | 메시지 유실 방지 | 낮음 |
| A10 | delivery | `DeliveryLogController` — Repository 직접 주입 (헥사고날 위반) | 아키텍처 일관성 | 중간 |

### 2.2 문서화 대상 (코드 미수정)

| # | 이슈 | 문서화 내용 |
|---|------|------------|
| B1 | 전체 `SecurityConfig permitAll()` | K8s NetworkPolicy + 서비스메시(Istio) 조합으로 해결하는 방향 설명 |
| B2 | user-service 라우트 JWT 인증 없음 (gateway) | 등록/로그인 엔드포인트는 public이 맞음; `/api/keys/**`는 JWT 필터 추가 방향 설명 |
| B3 | k8s/secret.yaml 평문 시크릿 | Sealed Secrets 또는 External Secrets Operator 도입 방향 설명 |
| B4 | 통합 테스트 전무 | Testcontainers + Kafka EmbeddedBroker 도입 방향 설명 |
| B5 | Redis 패스워드 없음 | `requirepass` 설정 + Spring `spring.data.redis.password` 추가 방향 |
| B6 | Kafka `DeliveryResultPublisher` 실패 시 이벤트 유실 | Transactional Outbox Pattern 도입 방향 설명 |
| B7 | Analytics consumer `@RetryableTopic` 없음 | notification consumer와 동일하게 설정 방향 설명 |
| B8 | `DeliveryLog.notificationId` 유니크 제약 없음 | DB 레벨 유니크 인덱스 + 낙관적 락 조합 방향 설명 |
| B9 | API 버저닝 없음 | `/api/v1/` prefix 도입 방향 설명 |
| B10 | OpenAPI/Swagger 미설정 | `springdoc-openapi-starter-webmvc-ui` 추가 방향 설명 |
| B11 | 테넌트별 Rate Limiting 미구현 | Redis 기반 테넌트 키 Rate Limiter 방향 설명 |
| B12 | api-gateway Circuit Breaker fallback 컨트롤러 없음 | FallbackController 구현 방향 설명 |

---

## 3. 구현 상세 계획

### A1. DB 인덱스 추가

**대상 파일:**
- `delivery-service/.../entity/DeliveryLogEntity.java`
- `notification-service/.../entity/NotificationEntity.java`
- `user-service/.../entity/ApiKeyEntity.java`

**변경 내용:**
```java
// DeliveryLogEntity
@Table(name = "delivery_logs", indexes = {
    @Index(name = "idx_delivery_notification_id", columnList = "notificationId"),
    @Index(name = "idx_delivery_tenant_id", columnList = "tenantId")
})

// NotificationEntity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_notification_tenant_id_created", columnList = "tenantId, createdAt")
})
```

### A2. ProcessDeliveryService `@Transactional` 추가

**파일:** `delivery-service/.../service/ProcessDeliveryService.java`

`process()` 메서드에 `@Transactional` 적용. DB 저장 → Kafka 발행 순서 유지 (Kafka는 트랜잭션 범위 밖에 두거나 주석으로 한계 명시).

### A3. TenantService.register() `@Transactional` 추가

**파일:** `user-service/.../service/TenantService.java`

Tenant 저장 + User 저장을 하나의 트랜잭션으로 묶어 고아 데이터 방지.

### A4. JwtAuthenticationFilter 예외 처리

**파일:** `api-gateway/.../filter/JwtAuthenticationFilter.java`

JWT 파싱/검증 중 예외 발생 시 `chain.filter()` 대신 `401 Unauthorized` 응답 반환.

```java
try {
    // existing JWT validation logic
} catch (Exception e) {
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    return response.setComplete();
}
```

### A5. CreateNotificationRequest content @Size 추가

**파일:** `notification-service/.../dto/CreateNotificationRequest.java`

```java
@NotBlank
@Size(max = 2000, message = "content must not exceed 2000 characters")
private String content;
```

### A6. 비밀번호 복잡도 검증

**파일:** `user-service/.../dto/CreateUserRequest.java` (또는 RegisterRequest)

```java
@NotBlank
@Size(min = 8, max = 100)
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
    message = "Password must contain uppercase, lowercase, number, and special character"
)
private String password;
```

### A7. Analytics Redis parseLong 예외 처리

**파일:** `analytics-service/.../service/RealtimeStatsService.java` (또는 해당 Redis 조회 위치)

```java
try {
    return Long.parseLong(raw);
} catch (NumberFormatException e) {
    log.warn("Invalid Redis value: {}", raw);
    return 0L;
}
```

### A8. ZoneId.systemDefault() → ZoneOffset.UTC

**파일:** `analytics-service/.../service/RecordDeliveryEventService.java` (또는 날짜 처리 위치)

```java
// Before
LocalDate.now(ZoneId.systemDefault())
// After
LocalDate.now(ZoneOffset.UTC)
```

### A9. Kafka Producer 신뢰성 설정

**파일:**
- `notification-service/.../messaging/KafkaConfig.java`
- `delivery-service/.../messaging/KafkaProducerConfig.java`

```java
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
```

### A10. DeliveryLogController 헥사고날 정리

**파일:** `delivery-service/.../controller/DeliveryLogController.java`

Repository 직접 주입 제거 → `GetDeliveryLogUseCase` 입력 포트 신규 생성 → `GetDeliveryLogService` 구현 → Not Found 시 `BusinessException(ErrorCode.RESOURCE_NOT_FOUND)` 던지기 (ApiResponse 일관성 확보).

---

## 4. 문서화 전략

`docs/improvement-todo.md`에 B1~B12 항목 추가. 각 항목에:
- 현재 상태 설명
- 실무 해결 방법 (도구/패턴 이름 명시)
- 미적용 이유 (포트폴리오 범위 한계)

---

## 5. 구현 순서

```
1단계 (낮음 난이도 묶음 - A1~A9): 각 파일 수정, 단독 변경
2단계 (중간 난이도 - A10): 새 포트/서비스 클래스 생성 필요
3단계: improvement-todo.md P2 섹션 업데이트 (B1~B12 상세 추가)
4단계: 빌드 + 테스트 전체 통과 확인
```

---

## 6. 완료 기준

- [ ] A1~A10 코드 변경 완료
- [ ] `mvn clean test` 전체 통과 (61건 이상)
- [ ] `improvement-todo.md` P2 섹션 B항목 상세화 완료
- [ ] `mvn clean compile -DskipTests` 빌드 성공

---

## 7. 범위 외 항목

다음은 이번 Plan에서 의도적으로 제외:

- 통합 테스트 추가 (Testcontainers): 별도 Plan 필요
- OpenAPI/Swagger 설정: 별도 Plan 필요
- K8s Sealed Secrets 도입: 인프라 변경 필요
- 테넌트별 Rate Limiting: 별도 Plan 필요
