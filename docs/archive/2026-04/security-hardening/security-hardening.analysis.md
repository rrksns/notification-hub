# Gap Analysis: security-hardening

**분석일**: 2026-04-03
**Design 문서**: `docs/02-design/features/security-hardening.design.md`

---

## 종합 결과

| 항목 | 점수 |
|------|------|
| **Match Rate** | **90% (9/10)** |
| 아키텍처 준수 | 100% |
| 컨벤션 준수 | 100% |

---

## 항목별 결과

| 항목 | 설명 | 결과 | 비고 |
|------|------|:----:|------|
| A1 | DB 인덱스 (3개 Entity) | PASS | 인덱스명, 컬럼명 완전 일치 |
| A2 | ProcessDeliveryService @Transactional | PASS | Outbox 주석 포함 |
| A3 | TenantService @Transactional | PASS | |
| A4 | JwtAuthenticationFilter try-catch (500→401) | PASS | |
| A5 | CreateNotificationRequest @Size(max=2000) | PASS | |
| A6 | RegisterTenantRequest @Pattern (비밀번호 복잡도) | PASS | |
| A7 | RedisRealtimeCounterAdapter parseLongSafe | PASS | 로깅 포함 |
| A8 | ZoneId.systemDefault() → ZoneOffset.UTC | **FAIL** | 미구현 (상세 하단) |
| A9 | Kafka Producer 신뢰성 설정 (2개 파일) | PASS | acks=all, idempotence |
| A10 | DeliveryLogController 헥사고날 정리 | PASS | UseCase+Service+Controller 모두 일치 |

---

## 미구현 항목 상세

### A8: ZoneId.systemDefault() → ZoneOffset.UTC

**문제**: 구현 단계에서 `grep` 결과 "없음"으로 판단해 스킵했으나, gap-detector가 실제 사용 위치를 발견.

**발견된 위치:**

| 파일 | 라인 | 심각도 |
|------|------|--------|
| `analytics-service/.../messaging/DeliveryResultConsumer.java` | 31 | 높음 (프로덕션 경로) |
| `user-service/.../domain/model/Tenant.java` | 28 | 낮음 (도메인 모델) |
| `user-service/.../domain/model/ApiKey.java` | 29, 37, 48 | 낮음 |
| `user-service/.../domain/model/User.java` | 28 | 낮음 |
| `delivery-service/.../domain/model/DeliveryLog.java` | 44 | 낮음 |
| `notification-service/.../domain/model/Notification.java` | 34 | 낮음 |

**DeliveryResultConsumer.java:31** 은 `ZoneId.systemDefault()`를 직접 사용해 Kafka 이벤트 타임스탬프를 변환하며, 이 값이 analytics 일별 집계에 사용됨. 서버 타임존이 UTC가 아닌 환경에서 날짜 버킷 오류 발생 가능.

도메인 모델 6개는 `LocalDateTime.now()` 호출 시 시스템 타임존을 묵시적으로 사용.
