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
