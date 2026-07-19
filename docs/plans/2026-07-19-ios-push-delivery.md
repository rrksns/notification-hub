# iOS PUSH Delivery Follow-Up Plan

**Goal:** Enable iOS push delivery through the existing FCM HTTP v1 provider after Firebase is connected to APNs.

**Decision:** Do not add a separate backend sender for iOS in the first iOS phase. FCM can deliver to iOS registration tokens when the Firebase project has the iOS app and APNs authentication configured. The current `PUSH` contract can stay as `recipient = FCM registration token` and `content = notification body`.

**Current Backend Status:**
- `PUSH_PROVIDER=fcm` sends to FCM HTTP v1 with `message.token`.
- `FcmPushSender` uses a platform-neutral FCM registration token.
- The current payload uses `notification.title` and `notification.body`.
- Android manual delivery is still pending real Firebase credentials and an Android registration token.

## Phase 7.1: Firebase and APNs Prerequisites

**Owner:** Project operator.

**Steps:**
1. Register the iOS app in Firebase project settings.
2. Create or reuse an APNs authentication key in Apple Developer.
3. Upload the APNs key to Firebase Cloud Messaging settings.
4. Confirm the iOS app can issue an FCM registration token.
5. Keep APNs key id, team id, private key, service account JSON, and registration tokens out of git.

**Verification:**
- Firebase console shows the iOS app.
- Firebase console shows APNs authentication configured.
- iOS client logs a valid FCM registration token.

## Phase 7.2: Backend Contract

**Contract:**
- Channel: `PUSH`.
- Recipient: FCM registration token from Android or iOS client.
- Content: notification body text.
- Title: `FCM_TITLE` environment value.
- Provider: `PUSH_PROVIDER=fcm`.

**No backend schema change is required for the first iOS delivery test.**

## Phase 7.3: iOS Manual Delivery Verification

**Prerequisites:**
- `PUSH_PROVIDER=fcm`.
- `FCM_PROJECT_ID`.
- `GOOGLE_APPLICATION_CREDENTIALS` or `FCM_CREDENTIALS_JSON`.
- `FCM_TITLE`.
- iOS FCM registration token from the Firebase-connected app.

**Direct FCM test shape:**

```bash
jq -n \
  --arg token "IOS_FCM_REGISTRATION_TOKEN" \
  --arg title "${FCM_TITLE:-Notification Hub}" \
  --arg body "Notification Hub iOS FCM actual delivery test." \
  '{message:{token:$token, notification:{title:$title, body:$body}}}' \
| curl -sS -o /tmp/fcm-ios-response.json -w "%{http_code}\n" \
  -X POST "${FCM_API_URL:-https://fcm.googleapis.com/v1}/projects/${FCM_PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  --data @-
```

**Expected Result:**
- FCM returns `200 OK`.
- iOS device receives the notification.

## Phase 7.4: Optional Platform-Specific Payload

Do this only after the first iOS delivery works and a concrete iOS requirement appears.

Potential requirements:
- APNs sound.
- Badge count.
- Interruption level.
- Mutable content.
- Custom data payload.

Possible backend change:
- Add optional PUSH metadata to notification input.
- Add platform-specific payload construction inside `FcmPushSender`.
- Keep the default payload path platform-neutral.
