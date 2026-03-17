# Delivery Service 전체 플로우

## 현재 상태: 미구현 (Stub)

delivery-service는 Maven 모듈로 선언되어 있으나, **소스 코드가 아직 구현되지 않은 상태**입니다. `pom.xml`만 존재하며 `src/` 디렉토리가 없습니다.

---

## 역할 (설계 의도)

notification-service가 Kafka에 발행한 `NotificationEvent`를 소비하여 **실제 발송(Email/SMS/Push)을 처리**하는 서비스입니다. 발송 결과는 `DeliveryResultEvent`로 다시 Kafka에 발행합니다.

---

## 설계 의도 (pom.xml 기반)

| 의존성 | 용도 |
|--------|------|
| `spring-boot-starter-web` | REST API 제공 |
| `spring-boot-starter-data-jpa` | 발송 이력 저장 (MySQL) |
| `spring-kafka` | Kafka 이벤트 소비/발행 |
| `resilience4j` | 재시도, 서킷 브레이커 |
| `spring-cloud-starter-netflix-eureka-client` | 서비스 등록 |
| `common` | 공유 이벤트 모델 |

---

## 예상 플로우

```
[Kafka 토픽: "notifications"]
  ← notification-service가 발행한 NotificationEvent
        ↓
[Delivery Service - 예상 포트: 8083]
  ↓ channel 값에 따라 분기
  ├─ EMAIL → EmailSender (SendGrid / AWS SES)
  ├─ SMS   → SmsSender (Twilio)
  └─ PUSH  → PushSender (Firebase Cloud Messaging)
        ↓
  발송 이력 MySQL 저장
        ↓
  DeliveryResultEvent 발행 → [Kafka 토픽: "delivery-results"]
  ├─ 성공: DeliveryResultEvent.success(...)
  └─ 실패: DeliveryResultEvent.failure(..., reason)
```

---

## Kafka 토픽 구조

| 토픽 | 역할 |
|------|------|
| `notifications` | notification-service → delivery-service (소비) |
| `notifications.dlq` | 처리 실패한 메시지 보관 (Dead Letter Queue) |
| `delivery-results` | delivery-service → analytics-service (발행) |
| `delivery-results.dlq` | 발행 실패 메시지 보관 |

---

## 예상 구현 구조 (헥사고날 아키텍처)

```
delivery-service/
└── src/main/java/com/notificationhub/delivery/
    ├── application/service/
    │   └── DeliveryService.java         ← 채널별 발송 오케스트레이션
    ├── domain/
    │   ├── model/
    │   │   ├── DeliveryLog.java          ← 발송 이력 도메인
    │   │   └── DeliveryStatus.java       ← PENDING / SUCCESS / FAILED
    │   └── port/
    │       ├── in/DeliverNotificationUseCase.java
    │       └── out/
    │           ├── DeliveryLogRepository.java
    │           ├── DeliveryResultPublisher.java
    │           ├── EmailSender.java
    │           ├── SmsSender.java
    │           └── PushSender.java
    └── infrastructure/
        ├── messaging/
        │   ├── NotificationEventConsumer.java   ← Kafka 소비
        │   └── KafkaDeliveryResultPublisher.java ← Kafka 발행
        ├── sender/
        │   ├── SendGridEmailSender.java
        │   ├── TwilioSmsSender.java
        │   └── FcmPushSender.java
        └── persistence/
            └── DeliveryLogRepositoryAdapter.java
```

---

## 구현 예정 기능

- Kafka Consumer로 `notifications` 토픽 구독
- 채널별 외부 발송 어댑터 구현 (Email / SMS / Push)
- 발송 이력(DeliveryLog) MySQL 저장
- 실패 시 Resilience4j 재시도 (지수 백오프)
- 재시도 초과 시 DLQ 처리
- 발송 결과를 `DeliveryResultEvent`로 Kafka 발행

---

## 기술 스택 (예정)

| 항목 | 내용 |
|------|------|
| 서버 포트 | 8083 (예정) |
| DB | MySQL (발송 이력) |
| 메시지 큐 | Kafka (이벤트 소비 및 결과 발행) |
| 장애 처리 | Resilience4j (재시도 + 서킷 브레이커) |
| 서비스 등록 | Eureka |
| 아키텍처 | 헥사고날 (다른 서비스와 동일한 패턴 예정) |
