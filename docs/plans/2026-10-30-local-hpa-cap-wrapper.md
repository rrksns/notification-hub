# Local HPA Cap Wrapper Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 로컬 Kubernetes 검증 명령 전후에 두 HPA 상한과 Deployment replica를 안전하게 제한·복구하는 재사용 가능한 Bash 래퍼를 제공한다.

**Architecture:** `scripts/with-local-hpa-cap.sh`는 명시적인 Kubernetes context에서 고정된 두 HPA와 Deployment의 원래 값을 메모리에 보관한다. 검증 명령을 실행하는 동안만 값이 2를 초과하는 리소스를 제한하고, Bash trap으로 성공·실패·signal 경로에서 변경한 값만 복구한다.

**Tech Stack:** Bash, kubectl, Kubernetes HPA, shell test harness.

---

### Task 1: 실패하는 Bash 계약 테스트를 작성한다.

**Files:**

- Create: `scripts/test/with-local-hpa-cap-test.sh`
- Test: `scripts/test/with-local-hpa-cap-test.sh`

**Step 1: 가짜 kubectl fixture를 만든다.**

임시 디렉터리에 `kubectl` 실행 파일을 만들고, `get`에는 고정된 HPA 최대치와 Deployment replica를 반환하며 `patch`, `scale` 호출은 로그 파일에 남긴다. 테스트마다 `PATH`와 로그 파일을 격리한다.

**Step 2: 성공 경로를 실패하도록 작성한다.**

아직 없는 `scripts/with-local-hpa-cap.sh`를 아래처럼 실행한다.

```bash
PATH="$fake_bin:$PATH" scripts/with-local-hpa-cap.sh --context fake -- true
```

두 HPA patch, 두 Deployment scale, 원래 값 복구 순서를 assertion으로 고정한다. 구현 전에는 파일 부재로 실패해야 한다.

**Step 3: 실패와 입력 거부 계약을 작성한다.**

`false` 검증 명령도 복구를 남기고 exit 1을 반환해야 한다. `--context` 또는 `--` 뒤 명령이 없을 때는 `patch`와 `scale` 로그가 비어 있어야 한다. 최대치나 desired replica가 2 이하이면 해당 변경 호출이 없어야 한다.

**Step 4: signal 복구 계약을 작성한다.**

자식 검증 명령을 대기 상태로 두고 래퍼에 `TERM`을 보낸다. 종료 뒤 로그에 복구 호출이 있는지 확인한다.

**Step 5: RED를 확인한다.**

Run:

```bash
bash scripts/test/with-local-hpa-cap-test.sh
```

Expected: 래퍼 스크립트가 없어 성공 경로 assertion이 실패한다.

### Task 2: 최소 HPA 래퍼를 구현한다.

**Files:**

- Create: `scripts/with-local-hpa-cap.sh`
- Test: `scripts/test/with-local-hpa-cap-test.sh`

**Step 1: 인자와 사전 조건을 구현한다.**

`--context NAME`, 선택 `--namespace NAME`, `--` 뒤 명령만 허용한다. unknown option, 누락 context, 누락 명령은 usage와 exit 2를 반환한다. `kubectl` 존재와 context 연결, namespace·HPA·Deployment 존재를 확인한다.

**Step 2: 상태 기록과 변경을 구현한다.**

`kubectl get hpa ... -o jsonpath='{.spec.maxReplicas}'`와 `kubectl get deployment ... -o jsonpath='{.spec.replicas}'`로 값을 기록한다. 2를 초과하는 대상만 merge patch와 `kubectl scale`로 제한한다.

**Step 3: trap 기반 복구를 구현한다.**

변경 완료 뒤 `EXIT`, `INT`, `TERM` trap을 설치한다. cleanup은 변경 플래그가 참인 리소스만 원래 값으로 되돌리고, 복구 실패를 stderr와 최종 종료 상태에 반영한다. signal 처리 시 자식 프로세스를 종료한 뒤 cleanup을 한 번만 실행한다.

**Step 4: GREEN을 확인한다.**

Run:

```bash
bash scripts/test/with-local-hpa-cap-test.sh
bash -n scripts/with-local-hpa-cap.sh scripts/test/with-local-hpa-cap-test.sh
```

Expected: 모든 가짜 kubectl 계약 테스트가 통과하고 구문 오류가 없다.

### Task 3: 운영 문서와 기록을 갱신한다.

**Files:**

- Modify: `README.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: README에 실행 예를 추가한다.**

명시적 `--context orbstack`과 `--` 구분자, 임시 상한 2, 자동 복구 보장을 기록한다. HPA 최소치·매니페스트는 변경하지 않으며, 중단 후 HPA가 Pod 수를 재조정할 수 있음을 명시한다.

**Step 2: 체크리스트와 context notes를 완료 상태로 갱신한다.**

구현 및 실제 Maven 확인 결과를 실행 시점의 관측치로 남긴다.

### Task 4: 실제 로컬 Maven 검증을 실행한다.

**Files:**

- Modify: 실행 중 `orbstack` context의 HPA와 Deployment만 일시 변경

**Step 1: Base64 JWT를 준비한다.**

```bash
export JWT_SECRET=bm90aWZpY2F0aW9uLW91dGJveC1yZXBsaWNhLXRlc3Qtc2VjcmV0LTMyeA==
```

**Step 2: 래퍼로 전체 Maven을 실행한다.**

```bash
./scripts/with-local-hpa-cap.sh --context orbstack -- mvn test
```

Expected: Testcontainers E2E를 포함한 전체 Maven 검증이 통과하고, 명령 종료 후 HPA 최대치와 Deployment replica가 원래 값으로 복구된다.

**Step 3: 변경 범위를 검토하고 커밋한다.**

```bash
git diff --check
git add scripts/with-local-hpa-cap.sh scripts/test/with-local-hpa-cap-test.sh README.md checklist.md context-notes.md
git commit -m "운영: 로컬 HPA 제한 검증 래퍼 추가"
```
