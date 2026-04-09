# Completion Report: security-hardening

## Executive Summary

| 항목 | 내용 |
|------|------|
| Feature | security-hardening |
| 시작일 | 2026-04-03 |
| 완료일 | 2026-04-03 |
| Match Rate | **100% (10/10)** |
| 수정 파일 | 21개 (수정 15 + 신규 2 + A8 추가 수정 6) |
| 테스트 결과 | 61/61 통과 |

### 1.3 Value Delivered (4-Perspective)

| 관점 | 내용 |
|------|------|
| **Problem** | P0/P1 완료 후에도 DB 인덱스 누락, Kafka 메시지 유실 가능성, 입력 검증 불완전, 헥사고날 아키텍처 위반, 타임존 불일치 등 면접에서 지적받을 수 있는 P2 이슈가 잔존했음 |
| **Solution** | 10개 항목(A1~A10)을 모두 코드로 구현 완료. `mvn test` 61건 전통과. ZoneId 이슈는 gap-detector가 추가 발견해 7개 파일 수정으로 100% 달성 |
| **Function / UX Effect** | DB 인덱스(notificationId, tenantId), @Transactional 2개, JWT 예외 401 처리, @Size/@Pattern 입력 검증, Kafka acks=all+idempotence, parseLongSafe, UTC 명시, 헥사고날 Controller 정리 — 실제 코드에 반영 |
| **Core Value** | 면접에서 "P2 항목도 실제로 고쳤다"를 61개 테스트와 함께 코드로 증명 가능한 포트폴리오 완성도 확보 |

---

## PDCA 진행 요약

```
[Plan] ✅ → [Design] ✅ → [Do] ✅ → [Check] ✅ (90%) → [A8 추가 수정] ✅ → [Check] ✅ (100%)
```

---

## 구현 완료 항목 (A1~A10)

| 항목 | 변경 내용 | 파일 |
|------|----------|------|
| A1 | DB 인덱스 `@Index` 추가 | DeliveryLogEntity, NotificationEntity, ApiKeyEntity |
| A2 | `ProcessDeliveryService.process()` `@Transactional` | ProcessDeliveryService.java |
| A3 | `TenantService.register()` `@Transactional` | TenantService.java |
| A4 | `JwtAuthenticationFilter` try-catch (500 → 401) | JwtAuthenticationFilter.java |
| A5 | `CreateNotificationRequest.content` `@Size(max=2000)` | CreateNotificationRequest.java |
| A6 | `RegisterTenantRequest.password` `@Pattern` (복잡도 검증) | RegisterTenantRequest.java |
| A7 | `RedisRealtimeCounterAdapter.parseLongSafe()` 추가 | RedisRealtimeCounterAdapter.java |
| A8 | `ZoneId.systemDefault()` → `ZoneOffset.UTC` (7개 파일) | DeliveryResultConsumer + 도메인 모델 6개 |
| A9 | Kafka Producer `acks=all`, `enable.idempotence=true` | KafkaConfig.java, KafkaProducerConfig.java |
| A10 | DeliveryLogController 헥사고날 정리 (UseCase 분리) | GetDeliveryLogUseCase(신규), GetDeliveryLogService(신규), DeliveryLogController(수정) |

---

## Gap 분석 → 추가 발견 및 수정 (A8)

gap-detector가 `grep` 결과와 달리 실제 사용 위치 7개를 추가 발견:

| 파일 | 문제 | 수정 |
|------|------|------|
| `DeliveryResultConsumer.java:31` | `ZoneId.systemDefault()` — analytics 날짜 집계 오류 위험 | `ZoneOffset.UTC` 로 변경 |
| `DeliveryLog.java:44` | `LocalDateTime.now()` | `LocalDateTime.now(ZoneOffset.UTC)` |
| `Notification.java:34` | `LocalDateTime.now()` | `LocalDateTime.now(ZoneOffset.UTC)` |
| `Tenant.java:28` | `LocalDateTime.now()` | `LocalDateTime.now(ZoneOffset.UTC)` |
| `ApiKey.java:29,37,48` | `LocalDateTime.now()` (3곳) | `LocalDateTime.now(ZoneOffset.UTC)` |
| `User.java:28` | `LocalDateTime.now()` | `LocalDateTime.now(ZoneOffset.UTC)` |

---

## 검증 결과

```
빌드:  mvn clean compile -DskipTests → BUILD SUCCESS (8모듈)
테스트: mvn test → 61/61 통과
  - user-service:         20건
  - notification-service: 12건
  - delivery-service:     12건
  - analytics-service:    17건
```

---

## 문서화 대상 항목 (B1~B12)

코드 미수정, "인지하고 있으며 개선 방향을 알고 있다"로 답변 가능한 항목들.
상세 내용은 `docs/improvement-todo.md` P2 섹션 참조.

| # | 이슈 | 개선 방향 |
|---|------|----------|
| B1 | 전체 `SecurityConfig permitAll()` | K8s NetworkPolicy + Istio mTLS 조합 |
| B2 | user-service 라우트 JWT 인증 없음 | `/api/keys/**`에 JwtAuthentication 필터 추가 |
| B3 | k8s/secret.yaml 평문 | Sealed Secrets / External Secrets Operator |
| B4 | 통합 테스트 전무 | Testcontainers + EmbeddedKafka |
| B5 | Redis 패스워드 없음 | requirepass 설정 |
| B6 | DeliveryResultPublisher 실패 시 이벤트 유실 | Transactional Outbox Pattern |
| B7 | Analytics consumer @RetryableTopic 없음 | notification consumer와 동일하게 설정 |
| B8 | DeliveryLog.notificationId 유니크 제약 없음 | DB 유니크 인덱스 + 낙관적 락 |
| B9 | API 버저닝 없음 | `/api/v1/` prefix 도입 |
| B10 | OpenAPI/Swagger 미설정 | springdoc-openapi-starter-webmvc-ui |
| B11 | 테넌트별 Rate Limiting 미구현 | Redis 기반 테넌트 키 Rate Limiter |
| B12 | api-gateway Circuit Breaker fallback 없음 | FallbackController 구현 |
