# SMS/PUSH Delivery Design

## Goal

delivery-service의 SMS와 PUSH 채널을 실제 외부 Provider로 발송할 수 있게 확장한다.

EMAIL에서 만든 Provider 선택 구조를 유지하면서 SMS는 Twilio, PUSH 1차는 Android FCM으로 구현한다. iOS PUSH는 FCM과 APNs 설정이 함께 필요하므로 2차 확장으로 분리한다.

## Assumptions

| 항목 | 결정 |
|------|------|
| SMS 1차 Provider | Twilio Programmable Messaging |
| PUSH 1차 Provider | Firebase Cloud Messaging HTTP v1 |
| PUSH 1차 대상 | Android |
| PUSH 2차 대상 | iOS. FCM + APNs 설정 필요 |
| 기본 Provider | `logging` |
| 실제 발송 Provider | `twilio`, `fcm` |
| 발송 계약 | 기존 `ChannelDelivererPort.deliver(channel, recipient, content)` 유지 |

## Architecture

현재 `ProcessDeliveryService`는 Kafka 이벤트를 받아 `ChannelDelivererPort`로 채널 발송을 위임하고, 성공 또는 실패 결과를 `DeliveryLog`와 `delivery-results` 이벤트로 남긴다.

이 흐름은 유지한다. 변경은 `delivery-service`의 infrastructure sender 계층 안에서만 진행한다.

```
ChannelDelivererAdapter
  ├─ EMAIL -> EmailSender
  ├─ SMS   -> SmsSender
  └─ PUSH  -> PushSender
```

각 채널 sender는 기본 logging 구현체와 실제 Provider 구현체를 가진다.

```
SmsSender
  ├─ LoggingSmsSender
  └─ TwilioSmsSender

PushSender
  ├─ LoggingPushSender
  └─ FcmPushSender
```

Provider 선택은 EMAIL과 같은 방식으로 Spring conditional bean을 사용한다.

## SMS Design

SMS는 Twilio를 1차 Provider로 사용한다.

설정은 환경변수 기반으로 둔다.

| 설정 | 환경변수 | 설명 |
|------|----------|------|
| `sms.provider` | `SMS_PROVIDER` | `logging` 또는 `twilio` |
| `twilio.account-sid` | `TWILIO_ACCOUNT_SID` | Twilio 계정 SID |
| `twilio.auth-token` | `TWILIO_AUTH_TOKEN` | Twilio 인증 토큰 |
| `twilio.from-number` | `TWILIO_FROM_NUMBER` | 발신 번호 |
| `twilio.messaging-service-sid` | `TWILIO_MESSAGING_SERVICE_SID` | Messaging Service 사용 시 발신 번호 대체 |
| `twilio.api-url` | `TWILIO_API_URL` | 테스트와 확장을 위한 API URL override |

초기 구현은 `from-number` 또는 `messaging-service-sid` 중 하나를 요구한다.

## Android Push Design

PUSH 1차는 Android FCM만 대상으로 한다.

초기 계약은 단순하게 유지한다. `NotificationEvent.recipient` 값을 Android FCM registration token으로 해석하고, `content`를 알림 본문으로 사용한다.

설정은 환경변수 기반으로 둔다.

| 설정 | 환경변수 | 설명 |
|------|----------|------|
| `push.provider` | `PUSH_PROVIDER` | `logging` 또는 `fcm` |
| `fcm.project-id` | `FCM_PROJECT_ID` | Firebase project id |
| `fcm.credentials-json` | `FCM_CREDENTIALS_JSON` | service account JSON 문자열 |
| `fcm.credentials-path` | `GOOGLE_APPLICATION_CREDENTIALS` | service account JSON 파일 경로 |
| `fcm.api-url` | `FCM_API_URL` | 테스트와 확장을 위한 API URL override |
| `fcm.title` | `FCM_TITLE` | 기본 알림 제목 |

인증은 service account 기반 OAuth access token을 사용한다. 실제 FCM 요청 로직과 token 발급 로직은 테스트 가능하도록 분리한다.

## iOS Push Follow-Up

iOS는 Android 1차 구현 이후 별도 Phase로 진행한다.

iOS 수신은 FCM만으로 끝나지 않고 Firebase 프로젝트에 APNs authentication key가 연결되어 있어야 한다. 백엔드 발송 API는 FCM HTTP v1을 유지할 수 있지만, iOS 앱 등록, APNs key 업로드, iOS FCM token 수집 계약이 추가로 필요하다.

## Error Handling

Provider 오류는 채널별 예외로 감싼다.

| 채널 | 예외 |
|------|------|
| EMAIL | `EmailDeliveryException` |
| SMS | `SmsDeliveryException` |
| PUSH | `PushDeliveryException` |

예외는 기존 delivery flow로 전파한다. `ProcessDeliveryService`가 실패를 받아 `DeliveryLog FAILED`와 실패 `DeliveryResultEvent`를 남기게 한다.

## Testing Strategy

단위 테스트는 외부 Provider를 호출하지 않는다.

- `ChannelDelivererAdapterTest`에서 SMS/PUSH 위임을 검증한다.
- Twilio sender 테스트는 mock server로 요청 body, 인증 헤더, 성공 status, 오류 status를 검증한다.
- FCM sender 테스트는 access token provider를 mock 처리하고 FCM request body와 오류 처리를 검증한다.
- 문서 갱신 후 `mvn test -pl delivery-service`를 실행한다.
- 최종 단계에서 전체 `mvn test`를 실행한다.

실제 발송 검증은 Provider 계정과 수신 대상이 준비된 뒤 수동으로 실행한다.

## Phase Order

1. Phase 0. 설계 문서와 체크리스트 작성.
2. Phase 1. SMS/PUSH sender 경계 분리.
3. Phase 2. SMS Twilio 설정 외부화.
4. Phase 3. Twilio SMS 실제 발송 구현.
5. Phase 4. Android FCM 설정 외부화.
6. Phase 5. Android FCM 실제 발송 구현.
7. Phase 6. 실패 처리와 문서 정리.
8. Phase 7. iOS PUSH 2차 확장.
