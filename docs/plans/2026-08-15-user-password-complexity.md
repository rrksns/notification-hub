# User Password Complexity Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 이미 적용된 `RegisterTenantRequest.password` 복잡도 검증을 테스트와 문서로 고정한다.

**Architecture:** production DTO annotation은 유지한다. Bean Validation `Validator`를 직접 사용하는 presentation DTO 단위 테스트를 추가해 약한 비밀번호가 실패하고 강한 비밀번호가 통과하는 contract만 검증한다.

**Tech Stack:** Java 17, Jakarta Bean Validation, JUnit 5, AssertJ.

---

### Task 1: DTO validation 테스트 작성

**Files:**
- Create: `user-service/src/test/java/com/notificationhub/user/presentation/dto/RegisterTenantRequestTest.java`

**Step 1: 강한 비밀번호 테스트 추가**

`ValidPass1!`를 가진 request를 검증하고 violation이 없음을 확인한다.

**Step 2: 약한 비밀번호 테스트 추가**

대문자, 소문자, 숫자, 특수문자가 각각 빠진 비밀번호를 검증하고 violation이 있음을 확인한다.

**Step 3: focused test 실행**

Run: `mvn test -pl user-service -am -Dtest=RegisterTenantRequestTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: 테스트 통과. production code는 이미 조건을 갖고 있다.

### Task 2: 문서와 상태 갱신

**Files:**
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: P2 상태 갱신**

P2 #22를 완료로 변경하고 DTO validation이 복잡도 조건을 검증한다고 기록한다.

**Step 2: 작업 노트 갱신**

이번 작업이 production code 변경 없이 테스트와 문서만 추가하는 범위였음을 기록한다.

### Task 3: 검증과 커밋

**Step 1: module test 실행**

Run: `mvn test -pl user-service -am`

Expected: user-service와 필요한 모듈 테스트 통과.

**Step 2: diff check 실행**

Run: `git diff --check`

Expected: output 없음.

**Step 3: 커밋과 push**

Run: `git add ...`

Run: `git commit -m "test: user 비밀번호 복잡도 검증 추가"`

Run: `git push`
