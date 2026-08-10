# Android FCM Data-Only Docs Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Android FCM 직접 검증 예시를 `android_noti_app`의 앱 로그 검증 계약에 맞춰 data-only payload 기준으로 정리한다.

**Architecture:** 서버 코드는 바꾸지 않는다. 문서에서 직접 FCM HTTP v1 호출과 `delivery-service` 경유 PUSH를 분리하고, 앱 내부 로그 검증에는 직접 data-only 호출을 쓰도록 안내한다.

**Tech Stack:** Markdown 문서, Firebase Cloud Messaging HTTP v1, 기존 `FcmPushSender` 문서.

---

### Task 1: 작업 범위 기록

**Files:**
- Create: `docs/plans/2026-08-10-android-fcm-data-only-docs-design.md`
- Create: `docs/plans/2026-08-10-android-fcm-data-only-docs.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: 설계와 구현 계획 작성**

Android 앱 로그 검증과 delivery-service provider 검증을 분리하는 문서 정리 계획을 저장한다.

**Step 2: 체크리스트 추가**

`checklist.md`에 Android FCM data-only 문서 정리 섹션을 추가한다.

### Task 2: manual_test Android 직접 호출 수정

**Files:**
- Modify: `manual_test.md`

**Step 1: 직접 호출 JSON 변경**

기존 `notification:{title,body}` 직접 호출 예시를 `data:{title,body,source}` 형태로 바꾼다.

**Step 2: 검증 목적 분리**

직접 data-only 호출은 Android 테스트 앱의 `onMessageReceived`, 앱 저장소, 최근 로그 UI 확인용이라고 설명한다.

### Task 3: README와 delivery-service flow 보강

**Files:**
- Modify: `README.md`
- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`

**Step 1: README FCM 설정 설명 보강**

`delivery-service` 경유는 notification payload이고 앱 로그 UI 검증은 data-only 직접 호출을 사용한다고 적는다.

**Step 2: delivery-service flow 설명 보강**

현재 `FcmPushSender` 요청 형태는 notification payload 그대로 두고, data-only 앱 로그 검증은 `manual_test.md`의 직접 호출 절차를 따르라고 적는다.

### Task 4: 검증

**Files:**
- Read: `manual_test.md`
- Read: `README.md`
- Read: `delivery-service/DELIVERY-SERVICE-FLOW.md`

**Step 1: 문서 검색 검증**

Run: `rg -n "data-only|notification payload|android_noti_app|message.data|message.notification" manual_test.md README.md delivery-service/DELIVERY-SERVICE-FLOW.md`

Expected: 직접 호출과 delivery-service 경유 설명이 분리되어 있다.

**Step 2: diff check**

Run: `git diff --check`

Expected: output 없음.

### Task 5: 커밋과 push

**Files:**
- Commit: `manual_test.md`
- Commit: `README.md`
- Commit: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Commit: `checklist.md`
- Commit: `context-notes.md`
- Commit: `docs/plans/2026-08-10-android-fcm-data-only-docs-design.md`
- Commit: `docs/plans/2026-08-10-android-fcm-data-only-docs.md`

**Step 1: 변경사항 stage**

Run: `git add manual_test.md README.md delivery-service/DELIVERY-SERVICE-FLOW.md checklist.md context-notes.md docs/plans/2026-08-10-android-fcm-data-only-docs-design.md docs/plans/2026-08-10-android-fcm-data-only-docs.md`

Expected: 문서 변경만 staged 상태다.

**Step 2: 커밋**

Run: `git commit -m "docs: align Android FCM data-only verification"`

Expected: Android FCM data-only 검증 문서 정리 커밋이 생성된다.

**Step 3: push**

Run: `git push`

Expected: 원격 `origin/main`에 커밋이 반영된다.
