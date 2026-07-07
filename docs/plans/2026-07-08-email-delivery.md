# Email Delivery Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the delivery-service EMAIL log stub with a real SendGrid-backed sender while preserving the existing Kafka, retry, circuit breaker, and delivery result flow.

**Architecture:** Keep `ProcessDeliveryService` and `ChannelDelivererPort` stable. Split the infrastructure sender layer so `ChannelDelivererAdapter` only routes by channel and EMAIL delivery is delegated to a dedicated email sender component, then add SendGrid behind that seam in a later phase.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, JUnit 5, Mockito, Resilience4j, SendGrid REST API.

---

## Phase 1: Split Email Sender Boundary

**Goal:** Prepare the delivery-service sender layer for SendGrid without changing application flow.

**Files:**
- Create: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/EmailSender.java`
- Create: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/ChannelDelivererAdapterTest.java`
- Modify: `delivery-service/src/main/java/com/notificationhub/delivery/infrastructure/sender/ChannelDelivererAdapter.java`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Steps:**
1. Write a failing test proving EMAIL delivery delegates to an `EmailSender`.
2. Run the focused test and confirm it fails because `EmailSender` does not exist yet.
3. Add the minimal `EmailSender` interface.
4. Inject `EmailSender` into `ChannelDelivererAdapter` and delegate EMAIL delivery to it.
5. Keep SMS and PUSH as log stubs for now.
6. Run `mvn test -pl delivery-service -Dtest=ChannelDelivererAdapterTest`.
7. Run `mvn test -pl delivery-service`.
8. Commit the Phase 1 change if verification passes.

## Later Phases

### Phase 2: Externalize SendGrid Configuration

Status: Complete. Added environment-backed properties for provider selection, SendGrid API key, sender email, sender name, API URL, and subject. Secrets are not stored in source control.

### Phase 3: Implement SendGrid REST Sender

Status: Complete. Added `SendGridEmailSender` using Spring `RestClient`, conditional provider activation with `email.provider=sendgrid`, and SendGrid Mail Send request tests.

### Phase 4: Provider Tests and Failure Cases

Status: Complete. Covered successful request construction, provider 4xx/5xx responses, missing API key, missing sender email, and network errors.

### Phase 5: Documentation and Manual Verification

Status: Complete. Updated README, PROCESS, and delivery-service flow docs to remove stale stub-only wording for EMAIL and document required environment variables.
