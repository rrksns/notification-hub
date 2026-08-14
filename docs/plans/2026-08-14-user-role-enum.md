# User Role Enum Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `User.create()`의 `"ADMIN"` 하드코딩을 `UserRole` enum 참조로 바꾼다.

**Architecture:** public API, DB entity, mapper, JWT claim은 변경하지 않는다. 도메인 모델 내부의 기본 역할 생성 경로만 enum을 참조하게 해서 기존 문자열 값은 그대로 유지한다.

**Tech Stack:** Java 17, JUnit 5, AssertJ.

---

### Task 1: RED 도메인 테스트 작성

**Files:**
- Modify: `user-service/src/test/java/com/notificationhub/user/domain/UserTest.java`

**Step 1: 실패하는 테스트 추가**

`UserRole.ADMIN.name()`과 `User.create(...).getRole()`이 같은지 검증한다.

**Step 2: RED 확인**

Run: `mvn test -pl user-service -am -Dtest=UserTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: `UserRole`이 없어 컴파일 실패한다.

### Task 2: UserRole enum 추가

**Files:**
- Create: `user-service/src/main/java/com/notificationhub/user/domain/model/UserRole.java`
- Modify: `user-service/src/main/java/com/notificationhub/user/domain/model/User.java`

**Step 1: enum 추가**

`ADMIN` 값을 가진 `UserRole` enum을 추가한다.

**Step 2: User.create 변경**

`"ADMIN"` 문자열을 `UserRole.ADMIN.name()`으로 바꾼다.

**Step 3: GREEN 확인**

Run: `mvn test -pl user-service -am -Dtest=UserTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: `UserTest`가 통과한다.

### Task 3: 문서와 상태 갱신

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: P2 상태 갱신**

P2 #27을 완료로 변경하고 기본 역할 생성이 enum 참조로 바뀌었음을 기록한다.

**Step 2: 작업 노트 갱신**

필드 타입과 외부 contract를 유지한 이유를 기록한다.

### Task 4: 검증과 커밋

**Step 1: module test 실행**

Run: `mvn test -pl user-service -am`

Expected: user-service와 필요한 모듈 테스트 통과.

**Step 2: diff check 실행**

Run: `git diff --check`

Expected: output 없음.

**Step 3: 커밋과 push**

Run: `git add ...`

Run: `git commit -m "feat: user 기본 역할 enum 추가"`

Run: `git push`
