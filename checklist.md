# Email Delivery Checklist

- [x] Confirm SendGrid as the first real email provider.
- [x] Create implementation plan document.
- [x] Create context notes.
- [x] Write RED test for EMAIL sender delegation.
- [x] Run focused test and confirm expected failure.
- [x] Add `EmailSender` boundary.
- [x] Delegate EMAIL branch through `EmailSender`.
- [x] Verify focused delivery-service sender test.
- [x] Verify full delivery-service tests.
- [x] Commit Phase 1 change.

## Phase 2: SendGrid Configuration

- [x] Write RED test for email provider and SendGrid property binding.
- [x] Run focused property binding test and confirm expected failure.
- [x] Add email delivery and SendGrid configuration property records.
- [x] Register new configuration properties in delivery-service.
- [x] Add environment-backed values to `application.yml`.
- [x] Verify focused property binding test.
- [x] Verify full delivery-service tests.
- [x] Commit Phase 2 change.

## Phase 3: SendGrid REST Sender

- [x] Write RED tests for SendGrid request, provider errors, and missing settings.
- [x] Run focused SendGrid sender test and confirm expected failure.
- [x] Add conditional `EmailSender` beans for logging and SendGrid providers.
- [x] Implement `SendGridEmailSender` with Spring `RestClient`.
- [x] Add `sendgrid.subject` environment-backed setting.
- [x] Verify focused SendGrid sender tests.
- [x] Verify full delivery-service tests.
- [x] Commit Phase 3 change.

## Phase 4: Provider Failure Coverage

- [x] Write RED tests for network errors and missing from-email.
- [x] Run focused SendGrid sender test and confirm expected failure.
- [x] Wrap non-response RestClient failures in `EmailDeliveryException`.
- [x] Verify focused SendGrid sender tests.
- [x] Verify full delivery-service tests.
- [x] Commit Phase 4 change.

## Phase 5: Documentation and Manual Verification

- [x] Update README SendGrid/email provider status.
- [x] Update PROCESS delivery-service history and test counts.
- [x] Rewrite delivery-service flow document to match implemented code.
- [x] Mark email delivery plan Phase 5 complete.
- [x] Verify documentation no longer contains stale EMAIL stub wording.
- [x] Run delivery-service tests after documentation update.
- [x] Commit Phase 5 documentation update.

## Final Verification

- [x] Run full multi-module `mvn test`.
- [x] Fix test-environment issues found during full verification.
- [x] Re-run full multi-module `mvn test`.
- [x] Update test-count documentation from full verification output.
- [x] Commit final verification updates.

## SendGrid Manual Verification

- [x] Copy updated `.env` values into `.env.local`.
- [x] Ensure `EMAIL_PROVIDER=sendgrid` is set locally.
- [x] Confirm required SendGrid environment values load without printing secrets.
- [x] Observe expected `403` before Sender Identity verification.
- [x] Verify Sender Identity in SendGrid.
- [x] Re-run direct SendGrid Mail Send API test.
- [x] Confirm SendGrid returns `202 Accepted`.
- [x] Confirm test recipient receives the email.
- [x] Commit manual verification documentation.

# SMS/PUSH Delivery Checklist

## Phase 0: Design and Planning

- [x] Confirm SMS Provider as Twilio.
- [x] Confirm PUSH 1차 target as Android FCM.
- [x] Defer iOS PUSH to a later FCM + APNs phase.
- [x] Create SMS/PUSH design document.
- [x] Create SMS/PUSH implementation plan document.
- [x] Add SMS/PUSH checklist items.
- [x] Add SMS/PUSH context notes.
- [x] Commit Phase 0 documentation.

## Phase 1: Sender Boundary Split

- [x] Write RED tests for SMS/PUSH sender delegation.
- [x] Run focused sender test and confirm expected failure.
- [x] Add `SmsSender` boundary.
- [x] Add `PushSender` boundary.
- [x] Add `LoggingSmsSender`.
- [x] Add `LoggingPushSender`.
- [x] Delegate SMS/PUSH branches through their senders.
- [x] Verify focused sender test.
- [x] Verify full delivery-service tests.
- [x] Update docs and checklist.
- [x] Commit Phase 1 change.

## Phase 2: Twilio SMS Configuration

- [x] Write RED test for SMS provider and Twilio property binding.
- [x] Run focused property binding test and confirm expected failure.
- [x] Add `SmsDeliveryProperties`.
- [x] Add `TwilioProperties`.
- [x] Register SMS and Twilio properties in delivery-service.
- [x] Add environment-backed SMS and Twilio values to `application.yml`.
- [x] Verify focused property binding test.
- [x] Verify full delivery-service tests.
- [x] Update docs and checklist.
- [x] Commit Phase 2 change.

## Phase 3: Twilio SMS Sender

- [x] Write RED tests for Twilio request, provider errors, network errors, and missing settings.
- [x] Run focused Twilio sender test and confirm expected failure.
- [x] Add conditional SMS sender beans for logging and Twilio providers.
- [x] Implement `TwilioSmsSender` with Spring `RestClient`.
- [x] Add `SmsDeliveryException`.
- [x] Verify focused Twilio sender tests.
- [x] Verify full delivery-service tests.
- [x] Update docs and manual test instructions.
- [x] Commit Phase 3 change.

## Phase 4: Android FCM Configuration

- [x] Write RED test for PUSH provider and FCM property binding.
- [x] Run focused property binding test and confirm expected failure.
- [x] Add `PushDeliveryProperties`.
- [x] Add `FcmProperties`.
- [x] Register PUSH and FCM properties in delivery-service.
- [x] Add environment-backed PUSH and FCM values to `application.yml`.
- [x] Document that PUSH `recipient` is an Android FCM registration token for 1차.
- [x] Verify focused property binding test.
- [x] Verify full delivery-service tests.
- [x] Update docs and checklist.
- [x] Commit Phase 4 change.

## Phase 5: Android FCM Sender

- [x] Write RED tests for FCM request, auth token usage, provider errors, network errors, and missing settings.
- [x] Run focused FCM sender test and confirm expected failure.
- [x] Add conditional PUSH sender beans for logging and FCM providers.
- [x] Add `FcmAccessTokenProvider`.
- [x] Implement service account access token provider.
- [x] Implement `FcmPushSender` with Spring `RestClient`.
- [x] Add `PushDeliveryException`.
- [x] Verify focused FCM sender tests.
- [x] Verify full delivery-service tests.
- [x] Update docs and manual test instructions.
- [x] Commit Phase 5 change.

## Phase 6: Failure Flow and Final Documentation

- [ ] Review delivery failure tests for channel-agnostic coverage.
- [ ] Add SMS/PUSH failure-flow tests if existing tests do not cover provider exceptions.
- [ ] Run full delivery-service tests.
- [ ] Run full multi-module `mvn test`.
- [ ] Update README, PROCESS, delivery-service flow, and manual test docs.
- [ ] Commit Phase 6 documentation and verification.

## Phase 7: iOS PUSH Follow-Up

- [ ] Register iOS app in Firebase.
- [ ] Create or reuse APNs authentication key in Apple Developer.
- [ ] Upload APNs authentication key to Firebase Cloud Messaging settings.
- [ ] Define iOS FCM token collection contract.
- [ ] Decide whether iOS needs platform-specific FCM payload handling.
- [ ] Create separate implementation plan for iOS PUSH.
