# SMS/PUSH Delivery Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add real SMS and Android PUSH delivery to delivery-service while preserving the existing Kafka, retry, circuit breaker, and delivery result flow.

**Architecture:** Keep `ProcessDeliveryService` and `ChannelDelivererPort` stable. Extend the infrastructure sender layer so `ChannelDelivererAdapter` delegates SMS to `SmsSender` and PUSH to `PushSender`, then add Twilio and Android FCM behind provider-selected implementations.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, JUnit 5, Mockito, Resilience4j, Twilio REST API, Firebase Cloud Messaging HTTP v1.

---

## Phase 0: Design and Checklist

**Goal:** Capture the Android-first PUSH decision and define the execution order before code changes.

**Files:**
- Create: `docs/plans/2026-07-12-sms-push-delivery-design.md`
- Create: `docs/plans/2026-07-12-sms-push-delivery.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Document SMS Provider as Twilio.
2. Document PUSH 1차 Provider as Android FCM.
3. Document iOS PUSH as a later FCM + APNs extension.
4. Add Phase checklist items for SMS/PUSH.
5. Add context notes explaining the Provider and ordering decisions.
6. Review the documentation diff.
7. Commit the Phase 0 documentation.

## Phase 1: Split SMS/PUSH Sender Boundaries

**Goal:** Remove SMS/PUSH log stubs from `ChannelDelivererAdapter` without adding external API calls yet.

**Files:**
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/SmsSender.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/PushSender.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/LoggingSmsSender.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/LoggingPushSender.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/ChannelDelivererAdapter.java`
- Test: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/ChannelDelivererAdapterTest.java`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Write failing tests proving SMS delegates to `SmsSender` and PUSH delegates to `PushSender`.
2. Run `mvn test -pl delivery-service -Dtest=ChannelDelivererAdapterTest` and confirm the expected failure.
3. Add `SmsSender` and `PushSender`.
4. Add logging implementations for both senders.
5. Inject `EmailSender`, `SmsSender`, and `PushSender` into `ChannelDelivererAdapter`.
6. Replace SMS/PUSH private log methods with sender delegation.
7. Run `mvn test -pl delivery-service -Dtest=ChannelDelivererAdapterTest`.
8. Run `mvn test -pl delivery-service`.
9. Update docs and checklist.
10. Commit Phase 1.

## Phase 2: Externalize Twilio SMS Configuration

**Goal:** Add SMS Provider selection and Twilio settings while keeping local startup on `logging`.

**Files:**
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/SmsDeliveryProperties.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/TwilioProperties.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/DeliveryServiceApplication.java`
- Modify: `delivery-service/src/main/resources/application.yml`
- Test: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/TwilioPropertiesTest.java`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Write failing property binding tests for `sms.provider` and `twilio.*`.
2. Run the focused test and confirm the expected failure.
3. Add `SmsDeliveryProperties`.
4. Add `TwilioProperties`.
5. Register properties in `DeliveryServiceApplication`.
6. Add `sms.provider=${SMS_PROVIDER:logging}` to `application.yml`.
7. Add environment-backed Twilio settings to `application.yml`.
8. Run the focused property test.
9. Run `mvn test -pl delivery-service`.
10. Update docs and checklist.
11. Commit Phase 2.

## Phase 3: Implement Twilio SMS Sender

**Goal:** Send real SMS through Twilio when `SMS_PROVIDER=twilio`.

**Files:**
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/TwilioSmsSender.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/SmsDeliveryException.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/LoggingSmsSender.java`
- Test: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/TwilioSmsSenderTest.java`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Write failing tests for Twilio request body, authentication, success status, missing settings, provider errors, and network errors.
2. Run the focused sender test and confirm the expected failure.
3. Add `SmsDeliveryException`.
4. Add conditional `LoggingSmsSender` activation for missing or `logging` provider.
5. Implement `TwilioSmsSender` with Spring `RestClient`.
6. Validate required Twilio settings before sending.
7. Wrap Twilio 4xx/5xx and network errors in `SmsDeliveryException`.
8. Run the focused Twilio tests.
9. Run `mvn test -pl delivery-service`.
10. Update docs and manual test instructions.
11. Commit Phase 3.

## Phase 4: Externalize Android FCM Configuration

**Goal:** Add PUSH Provider selection and Android FCM settings while keeping local startup on `logging`.

**Files:**
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/PushDeliveryProperties.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/FcmProperties.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/DeliveryServiceApplication.java`
- Modify: `delivery-service/src/main/resources/application.yml`
- Test: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/FcmPropertiesTest.java`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Write failing property binding tests for `push.provider` and `fcm.*`.
2. Run the focused test and confirm the expected failure.
3. Add `PushDeliveryProperties`.
4. Add `FcmProperties`.
5. Register properties in `DeliveryServiceApplication`.
6. Add `push.provider=${PUSH_PROVIDER:logging}` to `application.yml`.
7. Add environment-backed FCM settings to `application.yml`.
8. Document that `recipient` is treated as an Android FCM registration token in Phase 4 and Phase 5.
9. Run the focused property test.
10. Run `mvn test -pl delivery-service`.
11. Update docs and checklist.
12. Commit Phase 4.

## Phase 5: Implement Android FCM Sender

**Goal:** Send real Android push notifications through FCM HTTP v1 when `PUSH_PROVIDER=fcm`.

**Files:**
- Modify: `pom.xml`
- Modify: `delivery-service/pom.xml`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/FcmPushSender.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/FcmAccessTokenProvider.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/GoogleServiceAccountAccessTokenProvider.java`
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/PushDeliveryException.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/LoggingPushSender.java`
- Test: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/FcmPushSenderTest.java`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Write failing tests for FCM request body, bearer token usage, success status, missing settings, provider errors, and network errors.
2. Run the focused sender test and confirm the expected failure.
3. Add `PushDeliveryException`.
4. Add `FcmAccessTokenProvider` so authentication can be mocked in sender tests.
5. Implement service account access token provider.
6. Implement `FcmPushSender` with Spring `RestClient`.
7. Use `recipient` as the Android FCM registration token.
8. Send `content` as the notification body and `fcm.title` as the notification title.
9. Run the focused FCM tests.
10. Run `mvn test -pl delivery-service`.
11. Update docs and manual test instructions.
12. Commit Phase 5.

## Phase 6: Failure Flow and Final Documentation

**Goal:** Verify SMS/PUSH failures flow through the same delivery result path as EMAIL.

**Files:**
- Test: delivery-service tests that cover `ProcessDeliveryService` failure behavior if gaps are found.
- Modify: `README.md`
- Modify: `PROCESS.md`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Review existing failure tests for `ProcessDeliveryService`.
2. Add focused tests only if SMS/PUSH provider exceptions are not already covered by channel-agnostic failure tests.
3. Run `mvn test -pl delivery-service`.
4. Run full `mvn test`.
5. Update README, PROCESS, flow docs, and manual test docs.
6. Commit Phase 6.

**Result:**
- Existing `ProcessDeliveryServiceTest` failure coverage was sufficient because provider exceptions are handled through the channel-agnostic `ChannelDelivererPort` boundary.
- No duplicate SMS/PUSH-specific application failure test was added.
- `mvn test -pl delivery-service` passed with 39 tests.
- Full multi-module `mvn test` passed.
- README, PROCESS, delivery-service flow, manual test docs, checklist, and context notes were updated.

## Phase 7: iOS PUSH Follow-Up

**Goal:** Extend PUSH support to iOS after Android FCM is working.

**Files:**
- Modify: `README.md`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`
- Create: `docs/plans/2026-07-19-ios-push-delivery.md`

**Steps:**
1. Register the iOS app in Firebase.
2. Create or reuse an APNs authentication key in Apple Developer.
3. Upload the APNs authentication key to Firebase Cloud Messaging settings.
4. Define the iOS FCM token collection contract.
5. Decide whether iOS needs a platform-specific payload branch.
6. Implement only after Android FCM sender is verified.

**Result:**
- Local Phase 7 planning and documentation were completed on 2026-07-19.
- iOS uses the same backend contract as Android: `channel=PUSH`, `recipient=FCM registration token`, `content=notification body`.
- No platform-specific payload branch is required for the first iOS delivery verification.
- Firebase iOS app registration, APNs authentication key setup, and actual iOS device delivery verification remain external manual steps.
- Detailed follow-up plan: `docs/plans/2026-07-19-ios-push-delivery.md`.
