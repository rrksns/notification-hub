# Analytics Service 전체 플로우

## 현재 상태: 미구현 (Stub)

analytics-service는 Maven 모듈로 선언되어 있으나, **소스 코드가 아직 구현되지 않은 상태**입니다. `pom.xml`만 존재하며 `src/` 디렉토리가 없습니다.

---

## 설계 의도 (pom.xml 기반)

`pom.xml`의 의존성으로부터 이 서비스의 역할과 구조를 파악할 수 있습니다.

### 의존성 분석

| 의존성 | 용도 |
|--------|------|
| `spring-boot-starter-web` | REST API 제공 |
| `spring-boot-starter-data-mongodb` | 분석 데이터 저장 (시계열) |
| `spring-boot-starter-data-redis` | 집계 지표 캐싱 |
| `spring-kafka` | Kafka 이벤트 구독 |
| `spring-cloud-starter-netflix-eureka-client` | 서비스 등록 |
| `common` | 공유 이벤트 모델 |

---

## 예상 아키텍처

```
[Kafka 토픽 구독]
  ├─ "notifications" 토픽  ← notification-service 발행
  └─ "delivery-results" 토픽 ← delivery-service 발행
        ↓
[Analytics Service - 예상 포트: 8084]
  ├─ 이벤트 수신 및 집계
  ├─ MongoDB에 분석 데이터 저장
  ├─ Redis에 지표 캐싱
  └─ REST API로 분석 데이터 조회 (/api/analytics/**)
        ↓
[api-gateway → /api/analytics/**]
```

---

## 예상 데이터 흐름

```
delivery-service → DeliveryResultEvent → Kafka
                                            ↓
                                   analytics-service
                                      ├─ 발송 성공/실패율 집계
                                      ├─ 채널별 통계
                                      ├─ 테넌트별 사용량
                                      └─ MongoDB 저장 + Redis 캐시
```

---

## 기술 스택 (예정)

| 항목 | 내용 |
|------|------|
| 서버 포트 | 8084 (예정) |
| DB | MongoDB (시계열 분석 데이터) |
| 캐시 | Redis (집계 지표) |
| 메시지 큐 | Kafka (이벤트 소비) |
| 서비스 등록 | Eureka |
| 아키텍처 | 헥사고날 (다른 서비스와 동일한 패턴 예정) |

---

## 구현 예정 기능

- 테넌트별 알림 발송 통계 (성공/실패 수, 성공률)
- 채널별 (EMAIL / SMS / PUSH) 발송 현황
- 시간대별 트래픽 분석
- 대시보드용 집계 지표 API
