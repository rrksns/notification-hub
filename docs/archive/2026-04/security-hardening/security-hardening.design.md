# Design: security-hardening

**참조 Plan**: `docs/01-plan/features/security-hardening.plan.md`
**작성일**: 2026-04-03

---

## 변경 파일 목록

| # | 파일 | 변경 유형 | 항목 |
|---|------|----------|------|
| 1 | `delivery-service/.../entity/DeliveryLogEntity.java` | 수정 | A1 |
| 2 | `notification-service/.../entity/NotificationEntity.java` | 수정 | A1 |
| 3 | `user-service/.../entity/ApiKeyEntity.java` | 수정 | A1 |
| 4 | `delivery-service/.../service/ProcessDeliveryService.java` | 수정 | A2 |
| 5 | `user-service/.../service/TenantService.java` | 수정 | A3 |
| 6 | `api-gateway/.../filter/JwtAuthenticationFilter.java` | 수정 | A4 |
| 7 | `notification-service/.../dto/CreateNotificationRequest.java` | 수정 | A5 |
| 8 | `user-service/.../dto/RegisterTenantRequest.java` | 수정 | A6 |
| 9 | `analytics-service/.../cache/RedisRealtimeCounterAdapter.java` | 수정 | A7 |
| 10 | `analytics-service/.../service/RecordDeliveryEventService.java` | 수정 | A8 |
| 11 | `notification-service/.../messaging/KafkaConfig.java` | 수정 | A9 |
| 12 | `delivery-service/.../messaging/KafkaProducerConfig.java` | 수정 | A9 |
| 13 | `delivery-service/domain/port/in/GetDeliveryLogUseCase.java` | **신규** | A10 |
| 14 | `delivery-service/application/service/GetDeliveryLogService.java` | **신규** | A10 |
| 15 | `delivery-service/.../controller/DeliveryLogController.java` | 수정 | A10 |

---

## A1. DB 인덱스 추가

### 현재 상태

3개 Entity 모두 `@Table(name = "...")` 만 있고 `indexes` 없음.

### 변경 설계

#### `DeliveryLogEntity.java`

```java
// Before
@Table(name = "delivery_logs")

// After
@Table(name = "delivery_logs", indexes = {
    @Index(name = "idx_delivery_notification_id", columnList = "notificationId"),
    @Index(name = "idx_delivery_tenant_id", columnList = "tenantId")
})
```

- `notificationId`: `findByNotificationId()` — 멱등성 체크 시 매 요청 조회
- `tenantId`: `findByTenantId()` — 테넌트별 조회 목록

#### `NotificationEntity.java`

```java
// Before
@Table(name = "notifications")

// After
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_tenant_id", columnList = "tenantId"),
    @Index(name = "idx_notification_idempotency", columnList = "idempotencyKey")
})
```

- `tenantId`: 테넌트별 조회
- `idempotencyKey`: 이미 `unique=true` 이지만 별도 인덱스로 명시 (unique constraint가 자동으로 인덱스를 생성하지만 이름을 명시적으로 관리)

> Note: `idempotencyKey`는 이미 `@Column(unique = true)`가 있어 DB 인덱스는 자동 생성됨. 명시적 `@Index`는 불필요 — 테넌트 인덱스만 추가.

#### `ApiKeyEntity.java`

```java
// Before
@Table(name = "api_keys")

// After
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_apikey_tenant_id", columnList = "tenantId")
})
```

- `tenantId`: `findByTenantId()` 조회용

---

## A2. ProcessDeliveryService `@Transactional` 추가

### 현재 상태

`process()` 메서드에 트랜잭션 없음. `save()` 3회 호출이 각각 별개 트랜잭션.

### 변경 설계

```java
// Before
@Override
public Result process(Command command) { ... }

// After
import org.springframework.transaction.annotation.Transactional;

@Override
@Transactional
public Result process(Command command) { ... }
```

**트랜잭션 범위 주의사항:**
- DB 저장 3건 (`save()` × 3)은 하나의 트랜잭션으로 묶임
- `channelDelivererPort.deliver()` — 외부 API 호출이므로 트랜잭션 범위 밖에 있으면 이상적이나, 현재 구조상 내부에 포함됨
- `deliveryResultPublisher.publishXxx()` — Kafka 발행은 트랜잭션과 별개 (Kafka는 JPA 트랜잭션에 참여하지 않음). 이는 알려진 한계이며 주석으로 명시.

**추가할 주석 (코드 내):**
```java
// NOTE: Kafka publish is outside the JPA transaction boundary.
// If publish fails after DB commit, analytics will miss this event.
// Transactional Outbox Pattern would be the production solution.
```

---

## A3. TenantService.register() `@Transactional` 추가

### 현재 상태

`register()` 메서드에 트랜잭션 없음. `tenantRepository.save()` 후 `userRepository.save()` 실패 시 Tenant만 저장된 고아 상태 발생.

### 변경 설계

```java
// Before
@Override
public Result register(Command command) { ... }

// After
import org.springframework.transaction.annotation.Transactional;

@Override
@Transactional
public Result register(Command command) { ... }
```

---

## A4. JwtAuthenticationFilter 예외 처리 (500 → 401)

### 현재 상태

`jwtTokenProvider.getTenantId(token)` 또는 `getSubject(token)` 등에서 예외 발생 시 (만료된 토큰, 잘못된 서명 등) 필터가 예외를 던져 500 Internal Server Error 반환.

`isValid(token)` 가 `false`를 반환하는 경우는 처리되어 있으나, 예외를 던지는 경우는 미처리.

### 변경 설계

```java
// Before
String token = authHeader.substring(7);
if (!jwtTokenProvider.isValid(token)) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
}

String tenantId = jwtTokenProvider.getTenantId(token);
String userId = jwtTokenProvider.getSubject(token);
return chain.filter(...);

// After
String token = authHeader.substring(7);
try {
    if (!jwtTokenProvider.isValid(token)) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    String tenantId = jwtTokenProvider.getTenantId(token);
    String userId = jwtTokenProvider.getSubject(token);
    return chain.filter(
        exchange.mutate()
            .request(r -> r.headers(headers -> {
                headers.remove("X-Tenant-Id");
                headers.remove("X-User-Id");
                headers.add("X-Tenant-Id", tenantId);
                headers.add("X-User-Id", userId);
            }))
            .build()
    );
} catch (Exception e) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
}
```

---

## A5. CreateNotificationRequest content `@Size` 추가

### 현재 상태

```java
public record CreateNotificationRequest(
        @NotBlank String channel,
        @NotBlank String recipient,
        @NotBlank String content,          // ← @Size 없음
        @NotBlank String idempotencyKey
) {}
```

`NotificationEntity`의 `@Column(length = 2000)`은 DB 레벨 제약이므로, DB 예외가 아닌 Bean Validation 예외로 400 응답을 내려야 함.

### 변경 설계

```java
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank String channel,
        @NotBlank String recipient,
        @NotBlank @Size(max = 2000, message = "content must not exceed 2000 characters") String content,
        @NotBlank String idempotencyKey
) {}
```

---

## A6. RegisterTenantRequest 비밀번호 복잡도 검증

### 현재 상태

```java
@NotBlank @Size(min = 8) String password
```

길이만 검증, 복잡도(대/소문자, 숫자, 특수문자) 검증 없음.

### 변경 설계

```java
import jakarta.validation.constraints.Pattern;

@NotBlank
@Size(min = 8, max = 100)
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
    message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character (@$!%*?&)"
)
String password
```

**정규식 설명:**
- `(?=.*[a-z])` — 소문자 1개 이상
- `(?=.*[A-Z])` — 대문자 1개 이상
- `(?=.*\\d)` — 숫자 1개 이상
- `(?=.*[@$!%*?&])` — 특수문자 1개 이상 (허용 문자 명시)

---

## A7. RedisRealtimeCounterAdapter parseLong 예외 처리

### 현재 상태

```java
public long getTotalSuccess(String tenantId) {
    String val = redisTemplate.opsForValue().get(String.format(TOTAL_SUCCESS_KEY, tenantId));
    return val == null ? 0L : Long.parseLong(val);  // ← NumberFormatException 가능
}
```

Redis 값이 비정상(버전 불일치, 데이터 오염 등)일 때 `NumberFormatException` → 500 에러.

### 변경 설계

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(RedisRealtimeCounterAdapter.class);

// 공통 파싱 헬퍼
private long parseLongSafe(String val, String key) {
    if (val == null) return 0L;
    try {
        return Long.parseLong(val);
    } catch (NumberFormatException e) {
        log.warn("Invalid Redis counter value for key={}: '{}'", key, val);
        return 0L;
    }
}

public long getTotalSuccess(String tenantId) {
    String key = String.format(TOTAL_SUCCESS_KEY, tenantId);
    return parseLongSafe(redisTemplate.opsForValue().get(key), key);
}

public long getTotalFailed(String tenantId) {
    String key = String.format(TOTAL_FAILURE_KEY, tenantId);
    return parseLongSafe(redisTemplate.opsForValue().get(key), key);
}
```

---

## A8. ZoneId.systemDefault() → ZoneOffset.UTC

### 현재 상태 파악

`RecordDeliveryEventService`는 `command.occurredAt().toLocalDate()`를 그대로 사용 — 이미 `OffsetDateTime` 또는 `LocalDateTime`이므로 별도 `ZoneId` 변환 없음.

Redis 카운터 키에 날짜가 없으므로 analytics-service 내 `ZoneId.systemDefault()` 사용 위치를 확인.

**실제 적용 대상:** `analytics-service` 내 날짜 계산 코드. `RecordDeliveryEventService`에는 직접적인 `ZoneId.systemDefault()` 사용이 없으나, 향후 `LocalDate.now()` 호출 시 반드시 UTC를 명시.

**적용 규칙:** 코드베이스 전체에서 `LocalDate.now()` 또는 `LocalDateTime.now()` 사용 시 반드시 `ZoneOffset.UTC` 를 인자로 전달.

```java
// 금지
LocalDate.now()
LocalDate.now(ZoneId.systemDefault())

// 허용
LocalDate.now(ZoneOffset.UTC)
LocalDateTime.now(ZoneOffset.UTC)
```

**현재 실제 사용 위치 검색 필요** — 구현 단계에서 `grep -r "LocalDate.now\|LocalDateTime.now"` 로 전수 확인 후 적용.

---

## A9. Kafka Producer 신뢰성 설정

### 현재 상태

두 서비스 모두 `BOOTSTRAP_SERVERS`, `KEY_SERIALIZER`, `VALUE_SERIALIZER` 3개 설정만 존재. `acks`, `idempotence`, `retries` 미설정.

### 변경 설계

#### `notification-service/KafkaConfig.java`

```java
// Before
return new DefaultKafkaProducerFactory<>(Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
));

// After
Map<String, Object> props = new HashMap<>();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
// Reliability settings
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
return new DefaultKafkaProducerFactory<>(props);
```

#### `delivery-service/KafkaProducerConfig.java`

동일한 4개 설정을 기존 `HashMap`에 추가:

```java
// 기존 3줄 뒤에 추가
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
```

**설정 의미:**
| 설정 | 값 | 효과 |
|------|-----|------|
| `acks` | `all` | 리더+모든 ISR 복제 후 응답 — 메시지 유실 방지 |
| `enable.idempotence` | `true` | 재시도 시 중복 발행 방지 (exactly-once 보장) |
| `max.in.flight.requests` | `5` | idempotence 활성화 시 최대 5 (기본값과 동일, 명시적 선언) |
| `retries` | `MAX_VALUE` | 일시적 브로커 장애 시 무한 재시도 |

---

## A10. DeliveryLogController 헥사고날 아키텍처 정리

### 현재 상태 문제

```java
// DeliveryLogController.java - 현재
private final DeliveryLogRepository deliveryLogRepository;  // ← output port 직접 주입

// 문제점:
// 1. Controller가 output port(Repository)를 알면 안 됨 → 헥사고날 원칙 위반
// 2. Not Found 시 ResponseEntity.notFound().build() — body 없는 404 응답
//    (다른 컨트롤러는 BusinessException → ApiResponse.error(...) 방식)
```

### 변경 설계

#### 신규: `GetDeliveryLogUseCase.java` (input port)

**경로:** `delivery-service/src/main/java/com/notificationhub/delivery/domain/port/in/GetDeliveryLogUseCase.java`

```java
package com.notificationhub.delivery.domain.port.in;

import com.notificationhub.delivery.domain.model.DeliveryLog;
import java.util.List;

public interface GetDeliveryLogUseCase {
    DeliveryLog getById(String id);
    List<DeliveryLog> getByTenantId(String tenantId);
}
```

#### 신규: `GetDeliveryLogService.java` (application service)

**경로:** `delivery-service/src/main/java/com/notificationhub/delivery/application/service/GetDeliveryLogService.java`

```java
package com.notificationhub.delivery.application.service;

import com.notificationhub.common.exception.BusinessException;
import com.notificationhub.common.exception.ErrorCode;
import com.notificationhub.delivery.domain.model.DeliveryLog;
import com.notificationhub.delivery.domain.port.in.GetDeliveryLogUseCase;
import com.notificationhub.delivery.domain.port.out.DeliveryLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetDeliveryLogService implements GetDeliveryLogUseCase {

    private final DeliveryLogRepository deliveryLogRepository;

    public GetDeliveryLogService(DeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Override
    public DeliveryLog getById(String id) {
        return deliveryLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<DeliveryLog> getByTenantId(String tenantId) {
        return deliveryLogRepository.findByTenantId(tenantId);
    }
}
```

#### 수정: `DeliveryLogController.java`

```java
// Before
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryLogController {

    private final DeliveryLogRepository deliveryLogRepository;

    public DeliveryLogController(DeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryLogResponse>> getById(@PathVariable("id") String id) {
        return deliveryLogRepository.findById(id)
                .map(log -> ResponseEntity.ok(ApiResponse.ok(DeliveryLogResponse.from(log))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse>>> getByTenant(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        List<DeliveryLogResponse> logs = deliveryLogRepository.findByTenantId(tenantId)
                .stream().map(DeliveryLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}

// After
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryLogController {

    private final GetDeliveryLogUseCase getDeliveryLogUseCase;

    public DeliveryLogController(GetDeliveryLogUseCase getDeliveryLogUseCase) {
        this.getDeliveryLogUseCase = getDeliveryLogUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryLogResponse>> getById(@PathVariable("id") String id) {
        DeliveryLog log = getDeliveryLogUseCase.getById(id);  // throws BusinessException if not found
        return ResponseEntity.ok(ApiResponse.ok(DeliveryLogResponse.from(log)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryLogResponse>>> getByTenant(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        List<DeliveryLogResponse> logs = getDeliveryLogUseCase.getByTenantId(tenantId)
                .stream().map(DeliveryLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
```

**개선 포인트:**
1. Controller → input port(`GetDeliveryLogUseCase`) 의존 — 헥사고날 원칙 준수
2. Not Found 시 `BusinessException(RESOURCE_NOT_FOUND)` → `GlobalExceptionHandler`가 `ApiResponse.error(...)` 형태의 404 응답 반환 — 다른 컨트롤러와 일관된 오류 형식

---

## 구현 순서 (권장)

```
1. A1  — Entity 3개 @Index 추가 (독립 변경)
2. A5  — CreateNotificationRequest @Size (독립 변경)
3. A6  — RegisterTenantRequest @Pattern (독립 변경)
4. A9  — Kafka Producer 설정 2개 파일 (독립 변경)
5. A2  — ProcessDeliveryService @Transactional (독립 변경)
6. A3  — TenantService @Transactional (독립 변경)
7. A4  — JwtAuthenticationFilter try-catch (독립 변경)
8. A7  — RedisRealtimeCounterAdapter parseLong (독립 변경)
9. A8  — ZoneId grep + 수정 (독립 변경)
10. A10 — GetDeliveryLogUseCase 신규 + Controller 수정 (의존 있음: 신규 먼저)
```

---

## 완료 기준 (검증 방법)

| 항목 | 검증 명령 |
|------|----------|
| 빌드 성공 | `mvn clean compile -DskipTests` → BUILD SUCCESS |
| 테스트 통과 | `mvn test` → 61건 이상 통과 |
| A10 신규 클래스 | `GetDeliveryLogService`, `GetDeliveryLogUseCase` 클래스 존재 확인 |
| A1 인덱스 | `ddl-auto=update` 서비스 재시작 후 DB 스키마 인덱스 확인 |
