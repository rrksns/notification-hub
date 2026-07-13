# Context Notes

## 2026-07-08

- SendGrid is confirmed as the first real email provider.
- Current `delivery-service` already has Kafka consumption, `DeliveryLog`, retry/circuit breaker annotations, result publishing, and delivery idempotency.
- Phase 1 is intentionally limited to splitting the EMAIL sender boundary. It does not call SendGrid yet.
- `ChannelDelivererPort` remains unchanged so the application service contract and existing tests stay stable.
- SMS and PUSH remain log stubs until their own provider phases.
- RED verification for Phase 1 failed at test compilation because `EmailSender` did not exist, which is the intended missing boundary.
- Mockito inline mock maker failed on the local JDK agent attach mechanism, so `ChannelDelivererAdapterTest` uses a small manual recording test double instead.
- `LoggingEmailSender` is a temporary Spring bean that preserves current EMAIL log behavior until the SendGrid sender replaces it.
- Existing delivery-service Mockito tests also hit the same inline mock maker issue. The service now opts into Mockito's subclass mock maker under test resources because these tests only mock interfaces.
- Phase 2 will add environment-backed settings only. It will not fail startup when `SENDGRID_API_KEY` is absent because the app still defaults to the logging sender until Phase 3.
- `email.provider` defaults to `logging`. `sendgrid.api-key` and `sendgrid.from-email` default to empty strings to keep local startup working before SendGrid is explicitly enabled.
- Phase 3 uses SendGrid's Mail Send endpoint with `POST /v3/mail/send`, bearer auth, `personalizations`, `from`, `subject`, and `text/plain` content. A `202 Accepted` response is treated as success.
- `sendgrid.subject` will be added with a default subject because the current `EmailSender` contract only receives recipient and body content.
- `LoggingEmailSender` is active only when `email.provider` is missing or set to `logging`; `SendGridEmailSender` is active when `email.provider=sendgrid`.
- SendGrid 4xx/5xx responses and missing required settings throw `EmailDeliveryException`, which the existing delivery flow catches and records as FAILED.
- Phase 4 focuses on provider failure coverage: client/network failures should also be wrapped as `EmailDeliveryException`, and missing `fromEmail` should be explicitly tested.
- Phase 5 updates README, PROCESS, and `delivery-service/DELIVERY-SERVICE-FLOW.md` so EMAIL is documented as SendGrid-capable while SMS/PUSH remain stubs.
- Full `mvn test` initially failed in user-service because Mockito inline mock maker cannot self-attach on the local JDK. The same subclass mock maker setting used in delivery-service is being applied to user, notification, and analytics tests.
- Full `mvn test` then passed across all modules: user 21, notification 13, delivery 21, analytics 18.
- SendGrid manual verification initially returned `403` because the sender address was not verified. After completing Sender Identity verification and refreshing `.env.local`, SendGrid returned `202 Accepted` and the recipient mailbox received the test email.

## 2026-07-12

- SMS/PUSH delivery will follow the EMAIL provider pattern instead of changing `ProcessDeliveryService` or `ChannelDelivererPort`.
- SMS 1차 Provider is Twilio because the current delivery contract already has a single recipient string that maps cleanly to a phone number.
- PUSH 1차 target is Android FCM. iOS is intentionally deferred because iOS delivery requires FCM plus APNs authentication setup in Firebase.
- Android FCM 1차 keeps the contract simple: `recipient` is treated as an FCM registration token and `content` is treated as the notification body.
- SMS/PUSH local defaults stay on `logging` so developers can run delivery-service without Twilio or Firebase secrets.
- Twilio and FCM credentials must remain environment-backed and must not be committed to source control.
- Provider-specific sender exceptions should flow into the existing delivery failure path so `DeliveryLog FAILED` and failure `DeliveryResultEvent` behavior remains consistent across EMAIL, SMS, and PUSH.

## 2026-07-13

- Phase 1 splits SMS/PUSH sender boundaries only. It deliberately does not add Twilio or FCM API calls yet.
- `ChannelDelivererAdapterTest` continues using manual recording doubles instead of Mockito to avoid the local JDK inline mock maker issue.
- RED verification failed at test compilation because `SmsSender`, `PushSender`, and the three-sender `ChannelDelivererAdapter` constructor did not exist, which matched the intended missing boundary.
- `LoggingSmsSender` and `LoggingPushSender` preserve the previous log-only behavior behind provider-specific sender interfaces.
