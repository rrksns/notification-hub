# Common Module

## 역할

모든 마이크로서비스가 공통으로 사용하는 **공유 라이브러리**입니다. JWT 인증, 예외 처리, API 응답 포맷, Kafka 이벤트 정의를 제공합니다.

---

## 디렉토리 구조

```
common/
└── src/main/java/com/notificationhub/common/
    ├── event/
    │   ├── NotificationEvent.java    ← 알림 생성 이벤트 (Kafka)
    │   └── DeliveryResultEvent.java  ← 발송 결과 이벤트 (Kafka)
    ├── exception/
    │   ├── ErrorCode.java            ← 에러 코드 + HTTP 상태 매핑
    │   ├── BusinessException.java    ← 공통 비즈니스 예외
    │   └── GlobalExceptionHandler.java ← @RestControllerAdvice
    ├── jwt/
    │   ├── JwtProperties.java        ← JWT 설정 바인딩
    │   └── JwtTokenProvider.java     ← JWT 생성/검증
    └── response/
        ├── ApiResponse.java          ← 표준 API 응답 래퍼
        └── PageResponse.java         ← 페이지네이션 응답
```

---

## 모듈별 상세

### 1. JWT 인증 (`jwt/`)

#### JwtProperties
`application.yml`의 `jwt.*` 설정을 바인딩합니다.
- `secret`: 32자 이상 필수 (Base64)
- `accessTokenExpireMs`: 액세스 토큰 만료 시간 (ms)
- `refreshTokenExpireMs`: 리프레시 토큰 만료 시간 (ms)

#### JwtTokenProvider

| 메서드 | 설명 |
|--------|------|
| `generateAccessToken(subject, tenantId, role)` | JWT 생성 (클레임: subject, tenantId, role) |
| `isValid(token)` | 서명 및 만료 검증 (boolean 반환) |
| `getSubject(token)` | userId 추출 |
| `getTenantId(token)` | tenantId 추출 |
| `parseToken(token)` | Claims 파싱 (실패 시 예외) |

---

### 2. 예외 처리 (`exception/`)

#### ErrorCode (에러 코드 목록)

| 코드 | HTTP 상태 | 설명 |
|------|----------|------|
| `INVALID_INPUT` | 400 | 잘못된 입력값 |
| `RESOURCE_NOT_FOUND` | 404 | 리소스 없음 |
| `DUPLICATE_RESOURCE` | 409 | 중복 리소스 |
| `UNAUTHORIZED` | 401 | 인증 실패 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류 |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰 |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰 |
| `DUPLICATE_EMAIL` | 409 | 이메일 중복 |
| `DUPLICATE_NOTIFICATION` | 409 | 알림 중복 (idempotency) |
| `NOTIFICATION_NOT_FOUND` | 404 | 알림 없음 |

#### GlobalExceptionHandler
모든 서비스에서 동일한 에러 응답 포맷을 보장합니다.

```
BusinessException          → ErrorCode에 따른 HTTP 상태 + 에러 메시지
MethodArgumentNotValidException → 400 + 첫 번째 필드 검증 오류 메시지
Exception                  → 500 + 일반 오류 메시지
```

---

### 3. 응답 포맷 (`response/`)

#### ApiResponse\<T\>
모든 서비스의 HTTP 응답 표준 포맷입니다.

```json
{
  "success": true,
  "message": "선택적 메시지",
  "data": { ... }
}
```

- `null` 필드는 JSON에서 제외 (`@JsonInclude(NON_NULL)`)
- `ApiResponse.ok(data)` — 성공 응답
- `ApiResponse.error(message)` — 실패 응답

#### PageResponse\<T\>
목록 조회 시 페이지네이션 응답 포맷입니다.

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

---

### 4. Kafka 이벤트 정의 (`event/`)

서비스 간 비동기 통신 계약입니다. **모든 서비스가 이 클래스를 공유**합니다.

#### NotificationEvent
notification-service → delivery-service 방향으로 발행됩니다.

| 필드 | 설명 |
|------|------|
| `notificationId` | 알림 고유 ID |
| `tenantId` | 테넌트 식별자 |
| `channel` | 발송 채널 (EMAIL/SMS/PUSH) |
| `recipient` | 수신자 주소 |
| `content` | 메시지 내용 |
| `idempotencyKey` | 중복 방지 키 |
| `occurredAt` | 이벤트 발생 시각 (자동 설정) |

#### DeliveryResultEvent
delivery-service → notification-service / analytics-service 방향으로 발행됩니다.

| 필드 | 설명 |
|------|------|
| `deliveryLogId` | 발송 시도 고유 ID |
| `notificationId` | 원본 알림 ID |
| `tenantId` | 테넌트 식별자 |
| `channel` | 사용된 채널 |
| `status` | SUCCESS / FAILED |
| `failureReason` | 실패 사유 (성공 시 null) |
| `occurredAt` | 이벤트 발생 시각 |

팩토리 메서드: `DeliveryResultEvent.success(...)` / `DeliveryResultEvent.failure(..., reason)`

---

## 서비스별 의존 관계

```
common
  ├─ api-gateway        (JwtTokenProvider)
  ├─ user-service       (JwtTokenProvider, ApiResponse, BusinessException)
  ├─ notification-service (ApiResponse, NotificationEvent, BusinessException, ErrorCode)
  ├─ delivery-service   (NotificationEvent, DeliveryResultEvent, ApiResponse)
  └─ analytics-service  (NotificationEvent, DeliveryResultEvent)
```
