# Local HPA Cap Live Gate Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 명시적으로 opt-in한 OrbStack live release gate만 기존 HPA 제한 래퍼로 감싸고, 성공·실패·중단 뒤 원래 상태를 복구한다.

**Architecture:** `live-release-gate.sh`가 선택 context를 모든 kubectl 호출에 전달한다. `--local-hpa-cap --context NAME`의 외부 실행만 기존 래퍼를 `exec`하고, `LOCAL_HPA_CAP_ACTIVE=true`인 내부 실행은 중첩 없이 gate 본문을 수행한다.

**Tech Stack:** Bash, kubectl, Kubernetes HPA, fake command shell tests.

---

### Task 1: Live gate CLI 계약 테스트를 RED로 작성한다.

**Files:**

- Create: `scripts/test/live-release-gate-test.sh`
- Test: `scripts/test/live-release-gate-test.sh`

**Step 1: 가짜 실행 파일 fixture를 만든다.**

임시 디렉터리의 `kubectl`, `curl`, `with-local-hpa-cap.sh`가 호출 인자와 환경을 로그로 기록하게 한다. `kubectl`은 gate가 요구하는 namespace, Deployment rollout, Ingress TLS Secret, debug Pod, exec 요청에 성공 응답을 반환한다.

**Step 2: 기본과 context 경로를 실패하도록 작성한다.**

기본 gate에서 wrapper 로그가 비어 있는지, `--context fake`에서 kubectl 로그가 `--context fake`를 포함하는지 assertion으로 고정한다.

**Step 3: opt-in과 fail-closed 경로를 실패하도록 작성한다.**

`--local-hpa-cap`만으로는 exit 2와 빈 wrapper 로그를 요구한다. `--local-hpa-cap --context fake`는 wrapper를 한 번 호출하고 내부 gate에 같은 context를 전달해야 한다. `LOCAL_HPA_CAP_ACTIVE=true` 경로는 wrapper 재호출이 없어야 한다.

**Step 4: RED를 확인한다.**

Run:

```bash
bash scripts/test/live-release-gate-test.sh
```

Expected: `--local-hpa-cap`을 아직 인식하지 않으므로 opt-in 계약이 실패한다.

### Task 2: 최소 opt-in 래핑을 구현한다.

**Files:**

- Modify: `scripts/release/live-release-gate.sh`
- Test: `scripts/test/live-release-gate-test.sh`

**Step 1: 옵션을 파싱한다.**

`--context NAME`과 `--local-hpa-cap`을 추가한다. context 없이 local cap을 요구하면 usage와 exit 2를 반환한다.

**Step 2: context 배열을 적용한다.**

`kubectl` 호출을 단일 배열로 구성해, context가 주어진 경우 cluster-info, namespace, rollout, Ingress, Secret, debug Pod 생성·대기·exec·cleanup에 같은 `--context`를 적용한다.

**Step 3: 한 번만 wrapper를 exec한다.**

local cap이 참이고 `LOCAL_HPA_CAP_ACTIVE`가 아니면, 현재 script 절대 경로와 기존 gate 인자를 보존한 내부 호출을 HPA 래퍼의 `--` 뒤에 전달한다. 내부 프로세스는 환경 표식으로 재래핑을 건너뛴다.

**Step 4: GREEN을 확인한다.**

Run:

```bash
bash scripts/test/live-release-gate-test.sh
bash scripts/test/with-local-hpa-cap-test.sh
bash -n scripts/with-local-hpa-cap.sh scripts/release/live-release-gate.sh scripts/test/*.sh
```

Expected: 기본·context·opt-in·내부 경로가 모두 통과하고 기존 HPA 복구 계약도 유지된다.

### Task 3: 운영 문서를 갱신한다.

**Files:**

- Modify: `README.md`
- Modify: `docs/operations/release-gate.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: 로컬 실행 예를 추가한다.**

NodePort, CA bundle, `ROLLOUT_TIMEOUT=600s`와 함께 `--local-hpa-cap --context orbstack` 예를 기록한다. 상용·원격 cluster에서는 이 옵션을 사용하지 않는다는 점을 명시한다.

**Step 2: 체크리스트와 판단 기록을 갱신한다.**

실제 OrbStack 실행 결과와 복구 전후 HPA·Deployment 값을 기록한다.

### Task 4: 실제 OrbStack gate를 검증하고 커밋한다.

**Files:**

- Modify: 실행 중 `orbstack` context의 두 HPA 및 Deployment만 일시 변경

**Step 1: 로컬 gate 환경을 준비한다.**

현재 Ingress 주소, HTTP/HTTPS NodePort, CA bundle 위치를 확인한다. 누락된 값은 HPA를 patch하기 전에 실패해야 한다.

**Step 2: opt-in gate를 실행한다.**

```bash
INGRESS_ADDRESS=127.0.0.1 \
INGRESS_HTTP_PORT=31193 \
INGRESS_HTTPS_PORT=31856 \
INGRESS_CA_CERT=<local-ca-bundle> \
ROLLOUT_TIMEOUT=600s \
bash scripts/release/live-release-gate.sh --local-hpa-cap --context orbstack
```

Expected: 여섯 Deployment rollout, HTTPS health, HTTP redirect, Gateway 허용, untrusted Pod 차단이 통과하고 HPA·Deployment 값이 원래대로 복구된다.

**Step 3: 변경 범위와 검증을 확인하고 커밋한다.**

```bash
git diff --check
git add scripts/release/live-release-gate.sh scripts/test/live-release-gate-test.sh README.md docs/operations/release-gate.md checklist.md context-notes.md
git commit -m "운영: 로컬 HPA 제한 gate 옵션 추가"
```
