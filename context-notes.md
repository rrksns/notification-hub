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

## 2026-07-14

- Phase 2 adds configuration binding only. Twilio API calls, credential validation, and SMS send error handling stay in Phase 3.
- `SmsDeliveryProperties` mirrors `EmailDeliveryProperties` so `SMS_PROVIDER` can select `logging` or `twilio`.
- `TwilioProperties` stores account SID, auth token, from number, Messaging Service SID, and API URL with empty secret defaults to keep local startup working.
- The Twilio API URL default is `https://api.twilio.com/2010-04-01`; the concrete Messages endpoint will be composed in the Twilio sender phase.

## 2026-07-15

- Phase 3 implements `TwilioSmsSender` using Twilio's Messages API path `/Accounts/{AccountSid}/Messages.json`.
- Twilio requests use HTTP Basic Auth with Account SID and Auth Token and send form-urlencoded fields `To`, `Body`, and either `From` or `MessagingServiceSid`.
- `TwilioSmsSender` treats `201 Created` as success and wraps provider responses and network failures in `SmsDeliveryException`.
- Required setting validation happens before the request: account SID, auth token, and either from number or Messaging Service SID.
- Actual Twilio SMS delivery is not manually verified yet because it requires real Twilio credentials and a test recipient phone number.

## 2026-07-16

- Phase 4 adds Android FCM configuration binding only. FCM HTTP v1 calls, service account token creation, and provider error handling stay in Phase 5.
- `PushDeliveryProperties` mirrors `EmailDeliveryProperties` and `SmsDeliveryProperties` so `PUSH_PROVIDER` can select `logging` or `fcm`.
- `FcmProperties` stores Firebase project id, service account JSON, service account path, FCM API URL, and default notification title.
- `GOOGLE_APPLICATION_CREDENTIALS` is mapped into `fcm.credentials-path` to match common Firebase service account usage without committing credential files.
- PUSH 1차 keeps the simple contract from the design: `recipient` is an Android FCM registration token.

## 2026-07-17

- Phase 5 implements Android FCM HTTP v1 sender using `POST /v1/projects/{projectId}/messages:send`.
- `FcmPushSender` sends `recipient` as `message.token`, `fcm.title` as `notification.title`, and `content` as `notification.body`.
- `FcmAccessTokenProvider` separates OAuth access token creation from request construction so sender tests do not need real Firebase credentials.
- `GoogleServiceAccountAccessTokenProvider` uses Google Auth Library with the `https://www.googleapis.com/auth/firebase.messaging` scope.
- Token provider credential lookup order is inline service account JSON, service account file path, then Application Default Credentials.
- `GoogleServiceAccountAccessTokenProvider` caches scoped credentials and uses `refreshIfExpired()` so each push send does not force a token refresh request.
- Added `google-auth-library-oauth2-http` because FCM HTTP v1 requests require OAuth 2.0 bearer tokens.
- Actual Android FCM delivery is not manually verified yet because it requires a Firebase project, service account, and Android registration token.

## 2026-07-18

- Phase 6 reviewed `ProcessDeliveryServiceTest` before adding any new tests.
- `ProcessDeliveryService` catches exceptions from `ChannelDelivererPort.deliver()` after the channel-specific sender layer, so EMAIL, SMS, and PUSH provider failures share the same failure path.
- The existing `process_deliveryFails_savesFailedAndPublishes` test already verifies that a provider exception stores `DeliveryLog FAILED` and publishes a failure `DeliveryResultEvent`.
- No SMS/PUSH-specific failure-flow application test was added because it would duplicate the same channel-agnostic behavior without increasing coverage.
- `mvn test -pl delivery-service` passed with 39 tests after Phase 6 documentation updates.
- Full multi-module `mvn test` passed after Phase 6 documentation updates. Verified counts are user 21, notification 13, delivery 39, and analytics 18.
- README, PROCESS, delivery-service flow, and manual test docs now document SendGrid, Twilio, and Android FCM provider support separately from actual external manual verification status.

## 2026-07-19

- Phase 7 remaining work is iOS PUSH follow-up.
- Firebase iOS app registration, APNs authentication key creation, and APNs key upload are external console tasks and cannot be completed from the local repository.
- The current `FcmPushSender` uses FCM HTTP v1 `message.token` and `notification` payload, so it is platform-neutral at the backend boundary.
- iOS 1차 contract keeps `channel=PUSH`, `recipient=iOS FCM registration token`, `content=notification body`, and `FCM_TITLE=notification title`.
- No platform-specific FCM payload branch is needed before the first iOS delivery test.
- A separate iOS PUSH follow-up plan was added at `docs/plans/2026-07-19-ios-push-delivery.md`.
- `mvn test -pl delivery-service` passed with 39 tests after the Phase 7 documentation and FCM naming update.

## 2026-07-20

- Apple Developer Program paid enrollment is deferred.
- The next practical external verification target is Android FCM because it does not require Apple Developer or APNs setup.
- Firebase official docs confirm FCM HTTP v1 sends to a device registration token at `POST https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`.
- Firebase official docs recommend `GOOGLE_APPLICATION_CREDENTIALS` for local service account authorization, and the required OAuth scope is `https://www.googleapis.com/auth/firebase.messaging`.
- Android FCM actual delivery still requires a Firebase project id, service account JSON, and Android FCM registration token in local-only `.env.local`.
- Added `scripts/verify-android-fcm-env.sh` so missing local FCM settings can be checked without printing secret values.
- Preflight against the repository root `.env.local` reported missing `PUSH_PROVIDER=fcm`, `FCM_PROJECT_ID`, service account credentials, and `ANDROID_FCM_REGISTRATION_TOKEN`.
- `bash -n scripts/verify-android-fcm-env.sh` passed.
- `mvn test -pl delivery-service` passed with 39 tests after the Android FCM verification documentation update.

## 2026-07-23

- Android FCM preflight passed after `.env.local` received FCM provider, project id, service account credentials, and Android registration token.
- The first retry failed because `GOOGLE_APPLICATION_CREDENTIALS` pointed to Android `google-services.json`, which does not contain `client_email` or `private_key`.
- After switching to Firebase service account private key JSON, OAuth access token issuance succeeded.
- Direct FCM HTTP v1 send returned `200 OK` and a message name for project `notification-hub-c680b`.
- Android device receipt still needs user-side confirmation.
- Added `.gitignore` patterns for Firebase admin SDK and service account JSON files because a private key JSON was placed inside the repository path.
