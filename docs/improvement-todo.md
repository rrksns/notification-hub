# Notification Hub — 개선 사항 To-Do List

**작성일**: 2026-03-23
**최종 수정**: 2026-03-24 (P0/P1 전체 수정 완료, 빌드 + 테스트 61/61 통과)
**기준**: 실무 관점 코드 리뷰 결과

> 포트폴리오 프로젝트이므로 모든 항목을 구현할 필요는 없습니다.
> 면접에서 "이 부분은 어떻게 개선하실 건가요?" 질문에 답할 수 있는 수준이면 충분합니다.
> **P0은 구현 권장**, P1은 설명할 수 있으면 OK, P2는 인지만 하고 있으면 됩니다.

---

## P0 — 반드시 수정 (보안/데이터 무결성 직결) ✅ 전체 완료

| # | 서비스 | 이슈 | 수정 내용 | 상태 |
|---|--------|------|----------|------|
| 1 | 전체 | 시크릿 하드코딩 | 전 서비스 application.yml을 `${DB_PASSWORD:nhub1234}`, `${JWT_SECRET:...}` 환경변수 참조로 변경. `.env` 파일 생성 (`.gitignore` 포함 확인 완료) | ✅ |
| 2 | 전체 | `X-Tenant-Id` 헤더 신뢰 | `JwtAuthenticationFilter`에서 클라이언트가 보낸 `X-Tenant-Id`, `X-User-Id` 헤더를 먼저 제거(`headers.remove()`) 후 JWT 클레임 기반으로 재주입 | ✅ |
| 3 | delivery | CircuitBreaker fallback이 성공으로 처리됨 | `deliverFallback()`에서 `RuntimeException`을 던지도록 수정 → 발송 실패가 정상적으로 FAILED 처리됨 | ✅ |
| 4 | delivery / analytics | Kafka `addTrustedPackages("*")` | 양쪽 `KafkaConsumerConfig`에서 `"com.notificationhub.common.event"` 로 제한 | ✅ |
| 5 | notification / delivery | Kafka 발행 에러 핸들링 없음 | `kafkaTemplate.send().get(5, SECONDS)` 동기 확인 + `ExecutionException`/`TimeoutException`/`InterruptedException` 예외 로깅 및 전파 | ✅ |

**수정 파일:**
- `.env` (신규)
- `api-gateway/src/main/resources/application.yml`
- `user-service/src/main/resources/application.yml`
- `notification-service/src/main/resources/application.yml`
- `delivery-service/src/main/resources/application.yml`
- `analytics-service/src/main/resources/application.yml`
- `api-gateway/.../filter/JwtAuthenticationFilter.java`
- `delivery-service/.../sender/ChannelDelivererAdapter.java`
- `delivery-service/.../messaging/KafkaConsumerConfig.java`
- `analytics-service/.../messaging/KafkaConsumerConfig.java`
- `notification-service/.../messaging/KafkaNotificationEventPublisher.java`
- `delivery-service/.../messaging/KafkaDeliveryResultPublisher.java`

---

## P1 — 수정 권장 (실무 신뢰도 향상) ✅ 전체 완료

| # | 서비스 | 이슈 | 수정 내용 | 상태 |
|---|--------|------|----------|------|
| 6 | analytics | DailyStats Race Condition | `DailyStatsRepository`에 `incrementSuccess()`/`incrementFailure()` 원자적 메서드 추가. 어댑터에서 `MongoTemplate`의 `$inc` + `upsert`로 구현. `RecordDeliveryEventService`에서 read-modify-write 패턴 제거 | ✅ |
| 7 | delivery | Consumer 멱등성 체크 없음 | `ProcessDeliveryService.process()`에서 `notificationId`로 기존 `DeliveryLog` 존재 여부 확인. 중복 시 기존 결과 반환하고 skip | ✅ |
| 8 | notification | `@Transactional` 누락 | `CreateNotificationService.create()`에 `@Transactional` 적용 | ✅ |
| 9 | user | 이메일 유니크 제약 범위 | `UserEntity`의 글로벌 유니크(`unique=true`) 제거 → `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"tenantId", "email"}))` 복합 유니크로 변경 | ✅ |
| 10 | user / notification / delivery | `ddl-auto: update` | `${DDL_AUTO:update}` 환경변수화. 로컬 기본값 `update`, 프로덕션 배포 시 `DDL_AUTO=validate` 설정 | ✅ |
| 11 | delivery | DLQ 소비자 없음 | `DlqConsumer` 신규 생성. `notifications.dlq` 토픽 소비 → `log.error()`로 실패 알림 기록 (이후 알림 연동 확장 가능) | ✅ |
| 12 | notification | GET API에서 도메인 모델 직접 반환 | `GetNotificationResponse` DTO(record) 생성. `NotificationController.getById()`에서 `Notification` → DTO 변환 후 반환 | ✅ |

**수정 파일:**
- `analytics-service/.../port/out/DailyStatsRepository.java` (메서드 추가)
- `analytics-service/.../adapter/DailyStatsRepositoryAdapter.java` ($inc upsert 구현)
- `analytics-service/.../service/RecordDeliveryEventService.java` (원자적 증가 사용)
- `analytics-service/.../RecordDeliveryEventServiceTest.java` (테스트 갱신)
- `delivery-service/.../service/ProcessDeliveryService.java` (멱등성 체크 추가)
- `delivery-service/.../messaging/DlqConsumer.java` (신규)
- `notification-service/.../service/CreateNotificationService.java` (@Transactional)
- `notification-service/.../dto/GetNotificationResponse.java` (신규)
- `notification-service/.../controller/NotificationController.java` (DTO 반환)
- `user-service/.../entity/UserEntity.java` (복합 유니크 제약)

---

## P2 — 개선하면 좋음 (운영/코드 품질)

즉시 문제가 되지는 않지만, 실무 경험을 어필하는 데 도움이 되는 항목들입니다.

| # | 서비스 | 이슈 | 현재 상태 | 수정 방향 | 상태 |
|---|--------|------|----------|----------|------|
| 13 | 전체 (JPA) | DB 인덱스 누락 | tenantId, email 등 조회 빈도 높은 컬럼에 인덱스 없음 | `@Index` 어노테이션 추가 | 미착수 |
| 14 | 전체 (JPA) | `@Version` 미적용 | 동시 업데이트 시 데이터 덮어쓰기 가능 | 엔티티에 `@Version private Long version` 추가 | 미착수 |
| 15 | user | Tenant ↔ User ↔ ApiKey FK 없음 | tenantId가 String — 참조 무결성 없음 | FK 제약 또는 최소한 애플리케이션 레벨 검증 | 미착수 |
| 16 | analytics | `Long.parseLong()` 예외 미처리 | Redis 값 비정상 시 NumberFormatException | try-catch + 기본값 0L 반환 | 미착수 |
| 17 | analytics | `ZoneId.systemDefault()` 사용 | 컨테이너 환경에서 시스템 타임존 보장 안 됨 | `ZoneId.of("UTC")` 또는 `ZoneOffset.UTC` 명시 | 미착수 |
| 18 | analytics | DailyStats 내부 `long[]` 배열 | index 0/1이 성공/실패인지 코드로만 파악 가능 | `ChannelCounter(long success, long failure)` 객체로 교체 | 미착수 |
| 19 | api-gateway | IP 기반 Rate Limiting만 존재 | NAT/프록시 뒤에서 동일 IP로 집계, X-Forwarded-For 미처리 | 테넌트별 Rate Limiting 추가, X-Forwarded-For 파싱 | 미착수 |
| 20 | api-gateway | Circuit Breaker fallback 미구현 | `forward:/fallback` 설정만 있고 실제 컨트롤러 없음 | FallbackController 구현 (503 Service Unavailable 반환) | 미착수 |
| 21 | notification | content 길이 제한 없음 | 수 MB 데이터 전송 가능 | `@Size(max = 2000)` 추가 | 미착수 |
| 22 | user | 비밀번호 복잡도 검증 없음 | `@Size(min=8)` 만 적용 | 정규식 또는 passay 라이브러리로 복잡도 규칙 추가 | 미착수 |
| 23 | analytics | `findByTenantId()` 페이징 없음 | 수백만 건 조회 시 OOM 위험 | `Pageable` 파라미터 추가 | 미착수 |
| 24 | delivery | `@Transactional` 누락 | DB 저장 → Kafka 발행 사이 불일치 가능 | 서비스 메서드에 트랜잭션 적용 | 미착수 |
| 25 | 전체 | SecurityConfig `permitAll()` | 서비스 직접 호출 시 인증 없이 접근 가능 | 각 서비스에도 JWT 필터 적용 또는 K8s NetworkPolicy로 격리 | 미착수 |
| 26 | api-gateway | JwtAuthenticationFilter 예외 처리 | 토큰 파싱 실패 시 500 에러 | try-catch로 401 반환 | 미착수 |
| 27 | user | Role `"ADMIN"` 하드코딩 | `User.create()`에서 항상 "ADMIN" | enum 또는 상수로 관리 | 미착수 |

---

## 검증 결과

```
빌드:  mvn clean compile -DskipTests → BUILD SUCCESS (8모듈)
테스트: mvn test → 61/61 통과 (user 20 + notification 12 + delivery 12 + analytics 17)
```

---

## 면접 대응 전략

- **P0**: "보안과 데이터 무결성 이슈를 인지하고 수정했습니다" (코드에 반영 완료)
- **P1**: "동시성, 멱등성, 트랜잭션 등 실무에서 발생하는 문제를 해결했습니다" (코드에 반영 완료)
- **P2**: "추가 개선 포인트로 인지하고 있으며, 우선순위에 따라 순차 적용할 계획입니다"
