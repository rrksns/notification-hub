# Delivery Service 전체 플로우

## 현재 상태

delivery-service는 Kafka `notifications` 토픽을 소비해 채널별 발송을 처리하고, 결과를 MySQL과 Kafka `delivery-results` 토픽에 기록하는 서비스입니다.

현재 EMAIL 채널은 두 Provider를 지원합니다.

| Provider | 활성 조건 | 동작 |
|----------|-----------|------|
| `logging` | `EMAIL_PROVIDER` 미설정 또는 `EMAIL_PROVIDER=logging` | 외부 발송 없이 로그 출력 |
| `sendgrid` | `EMAIL_PROVIDER=sendgrid` | SendGrid Mail Send API 호출 |

SMS와 PUSH는 아직 로그 스텁입니다.

---

## 역할

notification-service가 Kafka에 발행한 `NotificationEvent`를 소비하여 발송 이력을 만들고, 채널별 발송 결과를 `DeliveryResultEvent`로 발행합니다.

---

## 처리 플로우

```
[Kafka: notifications]
  ↓ NotificationEvent
[NotificationEventConsumer]
  ↓ ProcessDeliveryUseCase.Command
[ProcessDeliveryService]
  ├─ notificationId 중복 체크
  ├─ DeliveryLog(PENDING) 저장
  ├─ ChannelDelivererPort.deliver(channel, recipient, content)
  │  ├─ EMAIL → EmailSender
  │  │  ├─ LoggingEmailSender
  │  │  └─ SendGridEmailSender
  │  ├─ SMS → 로그 스텁
  │  └─ PUSH → 로그 스텁
  ├─ 성공: DeliveryLog(SUCCESS) 저장
  ├─ 실패: DeliveryLog(FAILED, reason) 저장
  └─ Kafka: delivery-results 발행
```

---

## EMAIL Provider 설정

`application.yml`은 환경변수 기반으로 설정됩니다.

| 환경변수 | 기본값 | 설명 |
|----------|--------|------|
| `EMAIL_PROVIDER` | `logging` | `logging` 또는 `sendgrid` |
| `SENDGRID_API_KEY` | 빈 값 | SendGrid API Key. `sendgrid` provider에서 필수 |
| `SENDGRID_FROM_EMAIL` | 빈 값 | SendGrid 발신자 이메일. `sendgrid` provider에서 필수 |
| `SENDGRID_FROM_NAME` | `Notification Hub` | 발신자 이름 |
| `SENDGRID_API_URL` | `https://api.sendgrid.com/v3/mail/send` | SendGrid Mail Send API URL |
| `SENDGRID_SUBJECT` | `Notification Hub Alert` | 기본 메일 제목 |

SendGrid 실제 발송 예시입니다.

```bash
export EMAIL_PROVIDER=sendgrid
export SENDGRID_API_KEY="{SendGrid API Key}"
export SENDGRID_FROM_EMAIL="no-reply@example.com"
export SENDGRID_FROM_NAME="Notification Hub"
export SENDGRID_SUBJECT="Notification Hub Alert"

mvn spring-boot:run -pl delivery-service
```

---

## SendGrid 발송 요청

`SendGridEmailSender`는 SendGrid Mail Send API에 다음 형태의 요청을 보냅니다.

```
POST https://api.sendgrid.com/v3/mail/send
Authorization: Bearer {SENDGRID_API_KEY}
Content-Type: application/json

{
  "personalizations": [
    {
      "to": [
        { "email": "{recipient}" }
      ]
    }
  ],
  "from": {
    "email": "{SENDGRID_FROM_EMAIL}",
    "name": "{SENDGRID_FROM_NAME}"
  },
  "subject": "{SENDGRID_SUBJECT}",
  "content": [
    {
      "type": "text/plain",
      "value": "{content}"
    }
  ]
}
```

`202 Accepted`를 성공으로 처리합니다. SendGrid 4xx/5xx 응답, 네트워크 오류, 필수 설정 누락은 `EmailDeliveryException`으로 감싸져 기존 delivery 실패 흐름으로 전달됩니다.

실제 SendGrid 계정 수동 검증에서는 Sender Identity 인증 전 `403` 응답이 발생했고, 인증 완료 후 동일 요청이 `202 Accepted`로 성공했습니다. 이후 테스트 수신 메일함에서 실제 메일 수신을 확인했습니다.

---

## 재시도와 실패 처리

```
ChannelDelivererAdapter.deliver()
  ├─ @Retry(channelDelivery): 최대 3회, 1s → 2s → 4s
  ├─ @CircuitBreaker(channelDelivery): 실패율 50% 초과 시 OPEN
  └─ 실패 예외 전파

ProcessDeliveryService
  ├─ 예외 수신
  ├─ DeliveryLog FAILED 저장
  └─ DeliveryResultEvent.failure 발행

Kafka @RetryableTopic
  ├─ notifications-retry-1000
  ├─ notifications-retry-2000
  └─ notifications.dlq
```

---

## Kafka 토픽 구조

| 토픽 | 역할 |
|------|------|
| `notifications` | notification-service → delivery-service 발송 요청 |
| `notifications-retry-1000` | 1차 재시도 |
| `notifications-retry-2000` | 2차 재시도 |
| `notifications.dlq` | 최종 실패 메시지 보관 및 로깅 |
| `delivery-results` | delivery-service → analytics-service 발송 결과 |

---

## 구현 구조

```
delivery-service/src/main/java/com/notificationhub/delivery/
├── application/service/
│   ├── ProcessDeliveryService.java
│   └── GetDeliveryLogService.java
├── domain/
│   ├── model/
│   │   ├── DeliveryLog.java
│   │   ├── DeliveryStatus.java
│   │   └── ChannelType.java
│   └── port/
│       ├── in/
│       │   ├── ProcessDeliveryUseCase.java
│       │   └── GetDeliveryLogUseCase.java
│       └── out/
│           ├── ChannelDelivererPort.java
│           ├── DeliveryLogRepository.java
│           └── DeliveryResultPublisher.java
├── infrastructure/
│   ├── messaging/
│   ├── persistence/
│   └── sender/
│       ├── ChannelDelivererAdapter.java
│       ├── EmailSender.java
│       ├── LoggingEmailSender.java
│       ├── SendGridEmailSender.java
│       ├── SendGridProperties.java
│       └── EmailDeliveryException.java
└── presentation/
    └── controller/DeliveryLogController.java
```

---

## 검증 명령

```bash
mvn test -pl delivery-service
mvn test -pl delivery-service -Dtest=SendGridEmailSenderTest
```

실제 외부 발송 검증은 SendGrid API Key와 인증된 Sender Identity가 필요합니다. `.env.local`에 `EMAIL_PROVIDER=sendgrid`, `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`을 설정한 뒤 실행합니다.
