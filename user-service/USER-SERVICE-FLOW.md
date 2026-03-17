# User Service 전체 플로우

## 아키텍처: 헥사고날 아키텍처 (Ports & Adapters)

```
외부 요청
    ↓
[Presentation Layer]  ← HTTP 요청/응답
    ↓
[Application Layer]   ← 비즈니스 로직 (Use Cases)
    ↓
[Domain Layer]        ← 핵심 도메인 모델 + Port 인터페이스
    ↓
[Infrastructure Layer] ← DB, JWT, Security 등 외부 구현체
```

---

## 디렉토리 구조

```
user-service/
├── pom.xml
├── src/main/
│   ├── java/com/notificationhub/user/
│   │   ├── UserServiceApplication.java
│   │   ├── application/service/
│   │   │   ├── AuthService.java
│   │   │   ├── TenantService.java
│   │   │   └── ApiKeyService.java
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Tenant.java
│   │   │   │   ├── User.java
│   │   │   │   ├── ApiKey.java
│   │   │   │   └── SubscriptionPlan.java (enum)
│   │   │   ├── exception/
│   │   │   │   ├── InvalidTenantException.java
│   │   │   │   └── InvalidUserException.java
│   │   │   └── port/
│   │   │       ├── in/                       ← Use Case 인터페이스
│   │   │       │   ├── AuthenticateUseCase.java
│   │   │       │   ├── RegisterTenantUseCase.java
│   │   │       │   └── CreateApiKeyUseCase.java
│   │   │       └── out/                      ← Repository/Provider 인터페이스
│   │   │           ├── TenantRepository.java
│   │   │           ├── UserRepository.java
│   │   │           ├── ApiKeyRepository.java
│   │   │           ├── PasswordEncoder.java
│   │   │           └── TokenProvider.java
│   │   ├── infrastructure/
│   │   │   ├── persistence/
│   │   │   │   ├── adapter/
│   │   │   │   │   ├── TenantRepositoryAdapter.java
│   │   │   │   │   ├── UserRepositoryAdapter.java
│   │   │   │   │   └── ApiKeyRepositoryAdapter.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── TenantEntity.java
│   │   │   │   │   ├── UserEntity.java
│   │   │   │   │   └── ApiKeyEntity.java
│   │   │   │   └── repository/
│   │   │   │       ├── TenantJpaRepository.java
│   │   │   │       ├── UserJpaRepository.java
│   │   │   │       └── ApiKeyJpaRepository.java
│   │   │   └── security/
│   │   │       ├── BCryptPasswordEncoderAdapter.java
│   │   │       ├── JwtTokenProviderAdapter.java
│   │   │       └── SecurityConfig.java
│   │   └── presentation/
│   │       ├── controller/
│   │       │   ├── AuthController.java
│   │       │   ├── TenantController.java
│   │       │   └── ApiKeyController.java
│   │       └── dto/
│   │           ├── LoginRequest.java
│   │           ├── RegisterTenantRequest.java
│   │           └── CreateApiKeyRequest.java
│   └── resources/
│       └── application.yml
└── src/test/
    └── java/com/notificationhub/user/
        ├── application/
        │   ├── AuthenticateUseCaseTest.java
        │   └── RegisterTenantUseCaseTest.java
        └── domain/
            ├── ApiKeyTest.java
            ├── TenantTest.java
            └── UserTest.java
```

---

## 핵심 도메인 모델

| 도메인 | 설명 |
|--------|------|
| `Tenant` | 서비스를 사용하는 회사/조직 (구독 플랜 보유) |
| `User` | Tenant 소속의 사용자 (역할 기반) |
| `ApiKey` | Tenant가 API 호출에 사용하는 키 (만료일 설정 가능) |
| `SubscriptionPlan` | FREE / BASIC / PREMIUM 구분 (enum) |

### DB 테이블

| 테이블 | 주요 컬럼 |
|--------|-----------|
| `tenants` | id, name, email, plan, active, createdAt |
| `users` | id, tenantId, email, encodedPassword, role, createdAt |
| `api_keys` | id, tenantId, name, keyValue, expiresAt, revoked, createdAt |

---

## 3가지 주요 플로우

### 1. Tenant 등록 (`POST /api/users/register`)

```
HTTP 요청
  → TenantController          (Presentation)
  → RegisterTenantUseCase     (Port/In 인터페이스)
  → TenantService             (Application, UseCase 구현)
  → Tenant 도메인 객체 생성 + 검증
  → TenantRepository          (Port/Out 인터페이스)
  → TenantRepositoryAdapter   (Infrastructure)
  → TenantJpaRepository
  → MySQL (tenants 테이블)
```

### 2. 사용자 인증 (`POST /api/auth/login`)

```
HTTP 요청 (email + password)
  → AuthController
  → AuthenticateUseCase
  → AuthService
    → UserRepository로 사용자 조회
    → BCryptPasswordEncoderAdapter로 비밀번호 검증
    → JwtTokenProviderAdapter로 JWT 발급
  → JWT 토큰 반환
```

### 3. API Key 생성 (`POST /api/keys`)

```
HTTP 요청 (name + 선택적 만료일)
  → ApiKeyController
  → CreateApiKeyUseCase
  → ApiKeyService
    → API 키 값 생성 (랜덤)
    → ApiKey 도메인 객체 생성
    → ApiKeyRepository 저장
  → 생성된 키 반환
```

---

## 헥사고날 아키텍처 핵심 개념

### Port (인터페이스)

- `port/in/` — **Use Case** 인터페이스: Application이 외부에 제공하는 기능 명세
- `port/out/` — **Repository/Provider** 인터페이스: Application이 외부에 요구하는 기능 명세

### Adapter (구현체)

- `presentation/controller/` — **Driving Adapter** (외부 → 도메인 방향)
- `infrastructure/persistence/adapter/` — **Driven Adapter** (도메인 → 외부 방향)

> 이 구조 덕분에 **도메인 레이어는 Spring, JPA, MySQL에 전혀 의존하지 않습니다.**
> MySQL을 MongoDB로 교체해도 `infrastructure` 레이어만 바꾸면 됩니다.

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 서버 포트 | 8081 |
| DB | MySQL (포트 3307, nhub/nhub1234) |
| 인증 | JWT (common 라이브러리 공유) |
| 서비스 등록 | Eureka (localhost:8761) |
| 보안 | Spring Security + BCrypt |
| ORM | JPA / Hibernate |
