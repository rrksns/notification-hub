# Android FCM Manual Verification Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Verify real Android PUSH delivery through the existing `PUSH_PROVIDER=fcm` delivery-service path.

**Architecture:** The backend already sends FCM HTTP v1 requests through `FcmPushSender`. This verification phase does not add a new sender or change the delivery contract. It prepares Firebase credentials, confirms an Android FCM registration token, verifies local environment readiness, then sends a real push message.

**Tech Stack:** Firebase Cloud Messaging HTTP v1, Firebase service account JSON, Google OAuth scope `https://www.googleapis.com/auth/firebase.messaging`, Android FCM registration token, Spring Boot delivery-service.

---

## Official References

- Firebase FCM HTTP v1 sends messages to targets including device registration tokens.
- Firebase HTTP v1 requests use `POST https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`.
- Server authorization can use `GOOGLE_APPLICATION_CREDENTIALS` that points to a service account JSON file.
- The required OAuth scope for FCM send is `https://www.googleapis.com/auth/firebase.messaging`.
- Android clients must retrieve the current FCM registration token and keep it fresh because it can rotate.

## Task 1: Firebase Project Prerequisites

**Files:**
- No repository file change.

**Steps:**
1. Open Firebase console.
2. Select the target Firebase project.
3. Confirm Cloud Messaging API is enabled.
4. Open Project settings > Service accounts.
5. Generate a new private key JSON if no local service account key exists.
6. Store the JSON file outside git.

**Verification:**
- `FCM_PROJECT_ID` is known.
- Service account JSON file exists locally.
- Secret file is not inside the repository.

## Task 2: Android Registration Token

**Files:**
- No repository file change.

**Steps:**
1. Install and run the Android app connected to the Firebase project.
2. Retrieve the current FCM registration token from the Android app.
3. Keep the token outside git.

**Verification:**
- `ANDROID_FCM_REGISTRATION_TOKEN` is available locally.

## Task 3: Local Environment Setup

**Files:**
- Modify local-only `.env.local`.
- Use: `scripts/verify-android-fcm-env.sh`

**Step 1: Add local values**

```bash
PUSH_PROVIDER=fcm
FCM_PROJECT_ID={Firebase project id}
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
FCM_TITLE=Notification Hub
ANDROID_FCM_REGISTRATION_TOKEN={Android FCM registration token}
```

`FCM_CREDENTIALS_JSON` can be used instead of `GOOGLE_APPLICATION_CREDENTIALS`, but a file path is easier to verify locally without printing secrets.

**Step 2: Run preflight**

```bash
scripts/verify-android-fcm-env.sh .env.local
```

**Expected:**

```text
Android FCM verification environment is ready.
```

## Task 4: Backend Unit Verification

**Files:**
- No repository file change.

**Command:**

```bash
mvn test -pl delivery-service
```

**Expected:**
- 39 tests pass.

## Task 5: Actual Android PUSH Verification

**Files:**
- Modify: `manual_test.md`
- Modify: `context-notes.md`
- Modify: `checklist.md`

**Steps:**
1. Load `.env.local`.
2. Start `delivery-service` with `PUSH_PROVIDER=fcm`.
3. Trigger a `PUSH` notification with `recipient=${ANDROID_FCM_REGISTRATION_TOKEN}`.
4. Confirm the Android device receives the notification.
5. Record the result in `manual_test.md`.

**Verification:**
- FCM API returns success through delivery-service logs and delivery log state.
- Android device receives the notification.

## Current Status on 2026-07-20

Local backend tests pass, but actual Android FCM delivery cannot be executed yet because `.env.local` does not currently contain FCM environment values or an Android FCM registration token.
