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

- [x] Review delivery failure tests for channel-agnostic coverage.
- [x] Confirm no extra SMS/PUSH failure-flow test is needed because existing channel-agnostic tests cover provider exceptions.
- [x] Run full delivery-service tests.
- [x] Run full multi-module `mvn test`.
- [x] Update README, PROCESS, delivery-service flow, and manual test docs.
- [x] Commit Phase 6 documentation and verification.

## Phase 7: iOS PUSH Follow-Up

- [ ] Register iOS app in Firebase.
- [ ] Create or reuse APNs authentication key in Apple Developer.
- [ ] Upload APNs authentication key to Firebase Cloud Messaging settings.
- [x] Define iOS FCM token collection contract.
- [x] Decide whether iOS needs platform-specific FCM payload handling.
- [x] Create separate implementation plan for iOS PUSH.
- [x] Update README, delivery-service flow, manual test docs, checklist, and context notes.
- [x] Verify delivery-service tests after Phase 7 documentation update.
- [x] Commit Phase 7 iOS PUSH follow-up documentation.

## Android FCM Actual Verification

- [x] Create Android FCM manual verification plan.
- [x] Add Android FCM environment preflight script.
- [x] Document Firebase console setup steps.
- [x] Document Android registration token setup steps.
- [x] Add FCM values to local `.env.local`.
- [x] Run Android FCM environment preflight and record missing local values.
- [x] Run Android FCM environment preflight successfully.
- [x] Verify delivery-service tests before actual external send.
- [x] Send actual Android PUSH through FCM.
- [x] Confirm Android device receives PUSH notification.
- [x] Update manual test result after actual send.
- [x] Commit Android FCM verification documentation.

## Android FCM Data-Only Docs

- [x] Create Android FCM data-only docs design.
- [x] Create Android FCM data-only docs implementation plan.
- [x] Update Android direct FCM HTTP v1 example to data-only payload.
- [x] Document the difference between direct data-only app-log verification and delivery-service notification PUSH.
- [x] Verify documentation search and diff check.
- [x] Commit and push Android FCM data-only documentation.

## API Gateway Fallback

- [x] Create API Gateway fallback design.
- [x] Create API Gateway fallback implementation plan.
- [x] Write RED fallback controller test.
- [x] Run focused test and confirm expected failure.
- [x] Add fallback controller returning 503 JSON response.
- [x] Verify focused api-gateway fallback test.
- [x] Update improvement todo, gateway flow, and context notes.
- [x] Verify api-gateway tests and diff check.
- [x] Commit and push API Gateway fallback implementation.

## Analytics Delivery Event Pageable

- [x] Create analytics delivery event pageable design.
- [x] Create analytics delivery event pageable implementation plan.
- [x] Write RED adapter test for pageable tenant lookup.
- [x] Run focused test and confirm expected failure.
- [x] Change delivery event repository port and Mongo repository to pageable.
- [x] Verify focused adapter test.
- [x] Update improvement todo and context notes.
- [x] Verify analytics-service tests and diff check.
- [x] Commit and push analytics delivery event pageable change.

## User Role Enum

- [x] Create user role enum design.
- [x] Create user role enum implementation plan.
- [x] Write RED user role domain test.
- [x] Run focused test and confirm expected failure.
- [x] Add UserRole enum and replace role literal in User.create.
- [x] Verify focused user domain test.
- [x] Update improvement todo and context notes.
- [x] Verify user-service tests and diff check.
- [x] Commit and push user role enum change.

## User Password Complexity

- [x] Create user password complexity design.
- [x] Create user password complexity implementation plan.
- [x] Add RegisterTenantRequest validation test.
- [x] Verify focused DTO validation test.
- [x] Update improvement todo and context notes.
- [x] Verify user-service tests and diff check.
- [x] Commit and push user password complexity test.

## API Gateway Tenant Rate Limiting

- [x] Create API Gateway tenant rate limiting design.
- [x] Create API Gateway tenant rate limiting implementation plan.
- [x] Write RED tenant rate limit key resolver test.
- [x] Run focused key resolver test and confirm expected failure.
- [x] Add tenant-first rate limit key resolver.
- [x] Point gateway RequestRateLimiter to tenant-first resolver.
- [x] Verify focused api-gateway key resolver test.
- [x] Update improvement todo, gateway flow, Redis docs, and context notes.
- [x] Verify api-gateway tests and manual configuration surface.
- [x] Commit API Gateway tenant rate limiting change.

## Analytics DailyStats Channel Counter

- [x] Create analytics DailyStats channel counter design.
- [x] Create analytics DailyStats channel counter implementation plan.
- [x] Write RED DailyStats reconstruct test for ChannelStats counters.
- [x] Run focused DailyStats test and confirm expected failure.
- [x] Replace DailyStats internal long array counters with ChannelStats.
- [x] Preserve DailyStatsDocument Mongo array storage conversion.
- [x] Verify focused DailyStats domain test.
- [x] Update improvement todo, PROCESS, and context notes.
- [x] Verify analytics-service tests, full tests, and diff check.
- [x] Commit analytics DailyStats channel counter change.

## Commercialization Readiness Planning

- [x] Create commercialization priority list.
- [x] Create internal service access control design.
- [x] Create internal service access control implementation plan.
- [x] Verify documentation diff.
- [x] Commit commercialization planning documents.

## Internal Service Access Control

- [x] Create isolated worktree for implementation.
- [x] Verify baseline api-gateway tests.
- [x] Write RED gateway route policy test.
- [x] Run focused route policy test and confirm expected failure.
- [x] Split user-service gateway routes into register, auth, and API key routes.
- [x] Apply `JwtAuthentication` and `RequestRateLimiter` to `/api/keys/**`.
- [x] Keep `/api/users/register` public while preserving rate limiting.
- [x] Verify focused gateway route policy test.
- [x] Update gateway flow documentation and context notes.
- [x] Verify api-gateway tests and diff check.
- [x] Commit gateway route policy change.
- [x] Create isolated worktree for Task 2.
- [x] Verify baseline common module tests.
- [x] Write RED common JWT header filter tests.
- [x] Run focused common filter test and confirm expected failure.
- [x] Add Servlet JWT filter and security properties in common.
- [x] Verify focused common filter tests.
- [x] Update common flow documentation and context notes.
- [x] Verify common module tests and diff check.
- [x] Commit common JWT header filter change.
- [x] Create isolated worktree for Task 3.
- [x] Verify service security config paths and test pattern.
- [x] Write RED service SecurityConfig tests.
- [x] Run focused service security tests and confirm expected failure.
- [x] Register common JWT filter in user, notification, delivery, and analytics SecurityConfig.
- [x] Configure public actuator health and user public endpoint exclusions.
- [x] Verify focused service security tests.
- [x] Verify service module tests and diff check.
- [x] Update security documentation and context notes.
- [x] Commit service SecurityConfig change.
- [x] Create isolated worktree for Task 4.
- [x] Create K8s NetworkPolicy implementation plan.
- [x] Add internal service default-deny ingress NetworkPolicy.
- [x] Allow api-gateway Pod ingress to user, notification, delivery, and analytics service ports.
- [x] Allow approved Prometheus Pod ingress to internal service actuator ports.
- [x] Attempt NetworkPolicy kubectl dry-run and record local OrbStack API unavailable.
- [x] Validate NetworkPolicy manifest structure with local YAML checker.
- [x] Update security documentation and context notes for NetworkPolicy.
- [x] Commit NetworkPolicy change.

## JPA Optimistic Locking

- [x] Create JPA optimistic locking design.
- [x] Create JPA optimistic locking implementation plan.
- [x] Write RED entity version structure tests.
- [x] Run focused structure tests and confirm expected failure.
- [x] Add `@Version Long version` to JPA entities.
- [x] Verify focused structure tests.
- [x] Update improvement todo and context notes.
- [x] Verify target JPA module tests and diff check.
- [x] Commit and push JPA optimistic locking change.

## K8s Secret Hardening

- [x] Create isolated worktree for K8s Secret hardening.
- [x] Create K8s Secret hardening design.
- [x] Create K8s Secret hardening implementation plan.
- [x] Remove tracked `k8s/secret.yaml`.
- [x] Add ignored local `k8s/secret.yaml` rule.
- [x] Add tracked `k8s/secret.example.yaml` placeholder template.
- [x] Move MySQL and MongoDB password env values to `secretKeyRef`.
- [x] Update README deployment flow to create Secret with `kubectl create secret generic`.
- [x] Verify active files no longer contain removed K8s secret values.
- [x] Verify K8s YAML parsing and kubectl Secret generation.
- [x] Update context notes and manual test record.
- [x] Commit K8s Secret hardening change.

## DB Migration

- [x] Create isolated worktree for DB migration work.
- [x] Verify baseline full Maven tests.
- [x] Create DB migration design.
- [x] Create DB migration implementation plan.
- [x] Add Flyway MySQL dependency to JPA services.
- [x] Change JPA service default `DDL_AUTO` to `validate`.
- [x] Add initial Flyway SQL migrations for user, notification, and delivery schemas.
- [x] Update deployment and commercialization documentation.
- [x] Verify Maven tests and migration checks.
- [x] Commit DB migration change.

## Core E2E Integration Tests

- [x] Create isolated worktree for E2E integration test work.
- [x] Verify baseline full Maven tests.
- [x] Create E2E integration test design.
- [x] Create E2E integration test implementation plan.
- [x] Add dedicated `e2e-tests` Maven module.
- [x] Add notification-service Testcontainers acceptance test.
- [x] Verify notification-service E2E test.
- [x] Add delivery-service and analytics-service pipeline E2E test.
- [x] Verify delivery and analytics pipeline E2E test.
- [x] Update commercialization documentation and manual test record.
- [x] Verify full Maven tests and diff check.
- [x] Commit E2E integration test implementation.

## DLQ Ops Tool

- [x] Create isolated worktree for DLQ ops tool work.
- [x] Verify baseline full Maven tests.
- [x] Create DLQ ops tool design.
- [x] Create DLQ ops tool implementation plan.
- [x] Add dedicated `dlq-ops` Maven module.
- [x] Add minimal CLI skeleton and help surface.
- [x] Add CLI option parsing and event filters.
- [x] Add JSON Lines export/import codec.
- [x] Add DLQ list and export commands.
- [x] Add DLQ replay command with dry-run default.
- [x] Update operator documentation.
- [x] Verify focused `dlq-ops` tests.
- [x] Verify full Maven tests.
- [x] Manually exercise CLI help and invalid-command surfaces.
- [x] Commit DLQ ops tool implementation.

## CI Build Fix

- [x] Reproduce the clean reactor CI failure in the E2E test module.
- [x] Make service classes available to E2E test compilation.
- [x] Verify clean Maven reactor build.
- [x] Commit and push the CI build fix.

## Alerting

- [x] Explore existing Prometheus and application metrics.
- [x] Approve Webhook and SMTP delivery design.
- [x] Write alerting design document.
- [x] Write alerting implementation plan.
- [x] Add Prometheus alert rules.
- [ ] Add Alertmanager routing and receivers.
- [x] Connect Alertmanager to Docker Compose.
- [ ] Document environment variables and operations.
- [ ] Verify monitoring configuration and container readiness.
- [ ] Commit and push alerting implementation.
