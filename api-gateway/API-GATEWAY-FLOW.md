# API Gateway 전체 플로우

## 역할

모든 외부 요청의 단일 진입점(Single Entry Point). 인증, 라우팅, 속도 제한, 장애 격리를 중앙에서 처리하고 각 마이크로서비스로 요청을 전달합니다.

---

## 디렉토리 구조

```
api-gateway/
├── pom.xml
└── src/main/
    ├── java/com/notificationhub/gateway/
    │   ├── ApiGatewayApplication.java
    │   └── filter/
    │       └── JwtAuthenticationFilter.java
    └── resources/
        └── application.yml
```

---

## 라우팅 테이블

| 경로 | 대상 서비스 | 인증 | Rate Limit | 비고 |
|------|------------|------|------------|------|
| `/api/auth/**` | user-service | 없음 | 없음 | 공개 인증 엔드포인트 |
| `/api/users/**` | user-service | JWT | IP 기반 (100/s) | — |
| `/api/keys/**` | user-service | JWT | IP 기반 (100/s) | API 키 관리 |
| `/api/notifications/**` | notification-service | JWT | 없음 | X-Tenant-Id 헤더 주입 |
| `/api/deliveries/**` | delivery-service | JWT | 없음 | X-Tenant-Id 헤더 주입 |
| `/api/analytics/**` | analytics-service | JWT | 없음 | X-Tenant-Id 헤더 주입 |

---

## 전체 요청 플로우

```
클라이언트 요청
    ↓
[Spring Cloud Gateway - Port 8080]
    ↓
경로(Predicate) 매칭
    │
    ├─→ 공개 경로 (/api/auth/**)
    │   └─→ 필터 없이 user-service로 라우팅
    │
    └─→ 보호된 경로
        └─→ JwtAuthenticationFilter
            ├─ Authorization 헤더 추출 (Bearer 확인)
            ├─ JWT 서명 및 만료 검증
            │    └─ 실패 시 → 401 UNAUTHORIZED 즉시 반환
            ├─ tenantId, userId 추출
            └─ 요청 헤더에 X-Tenant-Id, X-User-Id 주입
                ↓
            Rate Limiter (Redis, IP 기반 - /api/users/**, /api/keys/**)
                ↓
            Circuit Breaker (Resilience4j)
            ├─ 정상 → 대상 서비스로 전달
            └─ 장애 → /fallback 으로 포워드
```

---

## JwtAuthenticationFilter 동작 상세

`filter/JwtAuthenticationFilter.java`는 `AbstractGatewayFilterFactory`를 상속한 커스텀 필터입니다.

```
1. Authorization 헤더 존재 확인
   → 없거나 "Bearer "로 시작 안 하면 → 401 반환

2. 토큰 추출 ("Bearer " 이후 문자열)

3. JwtTokenProvider.isValid(token) 검증
   → 유효하지 않으면 → 401 반환

4. 토큰에서 클레임 추출
   - tenantId → X-Tenant-Id 헤더
   - subject(userId) → X-User-Id 헤더

5. 헤더가 추가된 요청을 다음 필터 체인으로 전달
```

다운스트림 서비스들은 이 헤더로 **멀티 테넌시 격리**와 **사용자 컨텍스트**를 처리합니다.

---

## 장애 처리 (Resilience4j Circuit Breaker)

| 설정 | 값 |
|------|----|
| 슬라이딩 윈도우 크기 | 최근 요청 10개 |
| 실패율 임계값 | 50% (5회 이상 실패 시 OPEN) |
| OPEN 상태 대기 시간 | 10초 후 재시도 |
| Fallback | `/fallback` 엔드포인트로 포워드 |

---

## Rate Limiting (Redis)

토큰 버킷 알고리즘을 Redis로 구현합니다.

| 설정 | 값 |
|------|----|
| 알고리즘 | Token Bucket |
| replenishRate | 초당 100 토큰 보충 |
| burstCapacity | 최대 200 토큰 (순간 급증 허용) |
| Key | 클라이언트 IP 주소 |

---

## 서비스 디스커버리

- Eureka 서버: `http://localhost:8761/eureka/`
- 라우팅 URI 형식: `lb://service-name` (로드밸런싱 자동 처리)
- 서비스 등록/해제 및 헬스체크를 Eureka 하트비트로 관리

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 서버 포트 | 8080 |
| 라우팅 엔진 | Spring Cloud Gateway (Reactive) |
| 서비스 디스커버리 | Netflix Eureka Client |
| 속도 제한 | Redis (Reactive) |
| 장애 격리 | Resilience4j Circuit Breaker |
| 인증 | JWT (common 모듈 공유) |
| 모니터링 | Spring Boot Actuator + Prometheus |
