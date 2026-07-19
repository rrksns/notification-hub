# Delivery Service 전체 플로우

## 현재 상태

delivery-service는 Kafka `notifications` 토픽을 소비해 채널별 발송을 처리하고, 결과를 MySQL과 Kafka `delivery-results` 토픽에 기록하는 서비스입니다.

현재 EMAIL 채널은 두 Provider를 지원합니다.

| Provider | 활성 조건 | 동작 |
|----------|-----------|------|
| `logging` | `EMAIL_PROVIDER` 미설정 또는 `EMAIL_PROVIDER=logging` | 외부 발송 없이 로그 출력 |
| `sendgrid` | `EMAIL_PROVIDER=sendgrid` | SendGrid Mail Send API 호출 |

현재 SMS 채널은 두 Provider를 지원합니다.

| Provider | 활성 조건 | 동작 |
|----------|-----------|------|
| `logging` | `SMS_PROVIDER` 미설정 또는 `SMS_PROVIDER=logging` | 외부 발송 없이 로그 출력 |
| `twilio` | `SMS_PROVIDER=twilio` | Twilio Messages API 호출 |

현재 PUSH 채널은 두 Provider를 지원합니다.

| Provider | 활성 조건 | 동작 |
|----------|-----------|------|
| `logging` | `PUSH_PROVIDER` 미설정 또는 `PUSH_PROVIDER=logging` | 외부 발송 없이 로그 출력 |
| `fcm` | `PUSH_PROVIDER=fcm` | FCM HTTP v1 API 호출 |

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
  │  ├─ SMS → SmsSender
  │  │  ├─ LoggingSmsSender
  │  │  └─ TwilioSmsSender
  │  └─ PUSH → PushSender
  │     ├─ LoggingPushSender
  │     └─ FcmPushSender
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

## SMS Provider 설정

`application.yml`은 SMS Provider와 Twilio 값을 환경변수 기반으로 설정합니다.

| 환경변수 | 기본값 | 설명 |
|----------|--------|------|
| `SMS_PROVIDER` | `logging` | `logging` 또는 `twilio` |
| `TWILIO_ACCOUNT_SID` | 빈 값 | Twilio Account SID. `twilio` provider에서 필수 |
| `TWILIO_AUTH_TOKEN` | 빈 값 | Twilio Auth Token. `twilio` provider에서 필수 |
| `TWILIO_FROM_NUMBER` | 빈 값 | Twilio 발신 번호. Messaging Service 미사용 시 필수 |
| `TWILIO_MESSAGING_SERVICE_SID` | 빈 값 | Twilio Messaging Service SID. 발신 번호 대신 사용 가능 |
| `TWILIO_API_URL` | `https://api.twilio.com/2010-04-01` | Twilio API URL |

Twilio 실제 발송 예시입니다.

```bash
export SMS_PROVIDER=twilio
export TWILIO_ACCOUNT_SID="{Twilio Account SID}"
export TWILIO_AUTH_TOKEN="{Twilio Auth Token}"
export TWILIO_FROM_NUMBER="+15551234567"

mvn spring-boot:run -pl delivery-service
```

Messaging Service를 사용하는 경우 `TWILIO_FROM_NUMBER` 대신 `TWILIO_MESSAGING_SERVICE_SID`를 설정합니다.

---

## Twilio 발송 요청

`TwilioSmsSender`는 Twilio Messages API에 다음 형태의 요청을 보냅니다.

```
POST https://api.twilio.com/2010-04-01/Accounts/{TWILIO_ACCOUNT_SID}/Messages.json
Authorization: Basic base64({TWILIO_ACCOUNT_SID}:{TWILIO_AUTH_TOKEN})
Content-Type: application/x-www-form-urlencoded

To={recipient}
Body={content}
From={TWILIO_FROM_NUMBER}
```

Messaging Service를 사용하는 경우 `From` 대신 `MessagingServiceSid`를 보냅니다.

`201 Created`를 성공으로 처리합니다. Twilio 4xx/5xx 응답, 네트워크 오류, 필수 설정 누락은 `SmsDeliveryException`으로 감싸져 기존 delivery 실패 흐름으로 전달됩니다.

---

## FCM Provider 설정

`application.yml`은 PUSH Provider와 FCM 값을 환경변수 기반으로 설정합니다.

| 환경변수 | 기본값 | 설명 |
|----------|--------|------|
| `PUSH_PROVIDER` | `logging` | `logging` 또는 `fcm` |
| `FCM_PROJECT_ID` | 빈 값 | Firebase project id. `fcm` provider에서 필수 |
| `FCM_CREDENTIALS_JSON` | 빈 값 | Firebase service account JSON 문자열 |
| `GOOGLE_APPLICATION_CREDENTIALS` | 빈 값 | Firebase service account JSON 파일 경로 |
| `FCM_API_URL` | `https://fcm.googleapis.com/v1` | FCM HTTP v1 API URL |
| `FCM_TITLE` | `Notification Hub` | 기본 PUSH 알림 제목 |

PUSH 구현에서는 `NotificationEvent.recipient`를 Android 또는 iOS FCM registration token으로 해석합니다. iOS 발송은 Firebase iOS 앱 등록과 APNs authentication key 업로드가 완료된 뒤 같은 FCM provider로 검증합니다.

FCM 실제 발송 예시입니다.

```bash
export PUSH_PROVIDER=fcm
export FCM_PROJECT_ID="{Firebase Project ID}"
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
export FCM_TITLE="Notification Hub"

mvn spring-boot:run -pl delivery-service
```

service account JSON 문자열로 실행해야 하는 환경에서는 `GOOGLE_APPLICATION_CREDENTIALS` 대신 `FCM_CREDENTIALS_JSON`을 설정합니다.

---

## FCM 발송 요청

`FcmPushSender`는 FCM HTTP v1 API에 다음 형태의 요청을 보냅니다.

```
POST https://fcm.googleapis.com/v1/projects/{FCM_PROJECT_ID}/messages:send
Authorization: Bearer {OAuth Access Token}
Content-Type: application/json

{
  "message": {
    "token": "{recipient}",
    "notification": {
      "title": "{FCM_TITLE}",
      "body": "{content}"
    }
  }
}
```

`2xx` 응답을 성공으로 처리합니다. FCM 4xx/5xx 응답, 네트워크 오류, 필수 설정 누락은 `PushDeliveryException`으로 감싸져 기존 delivery 실패 흐름으로 전달됩니다.

`GoogleServiceAccountAccessTokenProvider`는 `FCM_CREDENTIALS_JSON`, `GOOGLE_APPLICATION_CREDENTIALS`, Application Default Credentials 순서로 service account 인증 정보를 찾고, `https://www.googleapis.com/auth/firebase.messaging` scope의 OAuth access token을 발급합니다.

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

`ProcessDeliveryService`의 실패 처리는 채널별 예외 타입에 의존하지 않습니다. `ChannelDelivererPort.deliver()`에서 발생한 `EmailDeliveryException`, `SmsDeliveryException`, `PushDeliveryException`은 모두 같은 catch 경로로 들어가며, `DeliveryLog FAILED` 저장과 실패 `DeliveryResultEvent` 발행으로 이어집니다.

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
│       ├── SmsSender.java
│       ├── PushSender.java
│       ├── EmailDeliveryProperties.java
│       ├── SmsDeliveryProperties.java
│       ├── PushDeliveryProperties.java
│       ├── LoggingEmailSender.java
│       ├── LoggingSmsSender.java
│       ├── LoggingPushSender.java
│       ├── SendGridEmailSender.java
│       ├── TwilioSmsSender.java
│       ├── FcmPushSender.java
│       ├── FcmAccessTokenProvider.java
│       ├── GoogleServiceAccountAccessTokenProvider.java
│       ├── SendGridProperties.java
│       ├── TwilioProperties.java
│       ├── FcmProperties.java
│       ├── EmailDeliveryException.java
│       ├── SmsDeliveryException.java
│       └── PushDeliveryException.java
└── presentation/
    └── controller/DeliveryLogController.java
```

---

## 검증 명령

```bash
mvn test -pl delivery-service
mvn test
mvn test -pl delivery-service -Dtest=ChannelDelivererAdapterTest
mvn test -pl delivery-service -Dtest=SendGridEmailSenderTest
mvn test -pl delivery-service -Dtest=TwilioPropertiesTest
mvn test -pl delivery-service -Dtest=TwilioSmsSenderTest
mvn test -pl delivery-service -Dtest=FcmPropertiesTest
mvn test -pl delivery-service -Dtest=FcmPushSenderTest
```

2026-07-18 기준 `mvn test -pl delivery-service`는 39개 테스트가 모두 통과했습니다. SMS/PUSH provider 실패 흐름은 `ProcessDeliveryServiceTest`의 채널 공통 실패 테스트로 검증합니다.

실제 외부 발송 검증은 SendGrid API Key와 인증된 Sender Identity가 필요합니다. `.env.local`에 `EMAIL_PROVIDER=sendgrid`, `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`을 설정한 뒤 실행합니다.
Twilio 실제 외부 발송 검증은 Twilio Account SID, Auth Token, 발신 번호 또는 Messaging Service SID, 수신 가능한 테스트 전화번호가 필요합니다. `.env.local`에 `SMS_PROVIDER=twilio`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` 또는 `TWILIO_MESSAGING_SERVICE_SID`를 설정한 뒤 실행합니다.
FCM 실제 외부 발송 검증은 Firebase project id, service account 인증 정보, Android 또는 iOS FCM registration token이 필요합니다. iOS는 Firebase 콘솔에서 iOS 앱과 APNs authentication key를 먼저 연결해야 합니다. `.env.local`에 `PUSH_PROVIDER=fcm`, `FCM_PROJECT_ID`, `GOOGLE_APPLICATION_CREDENTIALS` 또는 `FCM_CREDENTIALS_JSON`, `FCM_TITLE`을 설정한 뒤 실행합니다.
