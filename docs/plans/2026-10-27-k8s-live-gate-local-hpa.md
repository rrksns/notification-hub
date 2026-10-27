# Kubernetes Live Gate Local HPA Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 단일 OrbStack 노드의 HPA 기동 폭주를 일시적으로 제한하고, readiness 예산에 맞춘 live release gate를 실행해 실제 배포 증적을 갱신한다.

**Architecture:** 저장소의 HPA 매니페스트와 운영 정책은 변경하지 않는다. `user-service`와 `notification-service`의 실행 중 HPA만 최소·최대 2로 임시 제한하고, gate가 종료되면 기존 최소·최대 값으로 복원한다. Gate는 Kubernetes API, rollout, TLS, HTTP 리디렉션, NetworkPolicy 경계를 검증한다.

**Tech Stack:** Kubernetes, OrbStack, kubectl, Bash, NGINX Ingress, Spring Boot Actuator.

---

### Task 1: 현재 클러스터 원인과 원복 값을 기록

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1:** `kubectl get hpa -n notification-hub`로 원래 HPA 최소·최대 replica를 기록한다.

**Step 2:** `kubectl top node`와 Deployment 상태를 확인해 과부하 증거를 수집한다.

**Step 3:** readiness probe 초기 지연과 gate timeout을 비교한다.

### Task 2: 로컬 HPA를 임시 제한하고 rollout 검증

**Files:**
- Modify: 실행 중 Kubernetes HPA 리소스만 변경

**Step 1:** user와 notification HPA의 최소·최대 replica를 모두 2로 patch한다.

**Step 2:** 두 대상 Deployment를 2 replica로 scale한다.

**Step 3:** 여섯 애플리케이션 Deployment rollout을 600초 예산으로 확인한다.

**Step 4:** 실패 시 첫 실패 Deployment의 Pod describe, 최근 로그, events를 수집하고 추가 변경 없이 중단한다.

### Task 3: live release gate 실행과 HPA 복원

**Files:**
- Modify: `manual_test.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1:** 이전 리허설과 같은 local Ingress 주소와 NodePort, CA bundle을 확인한다.

**Step 2:** `ROLLOUT_TIMEOUT=600s`로 `scripts/release/live-release-gate.sh`를 실행한다.

**Step 3:** 성공·실패 출력을 기록한다.

**Step 4:** gate 결과와 관계없이 원래 HPA 최소·최대 값으로 복원하고 HPA 상태를 확인한다.

**Step 5:** `git diff --check`로 문서 변경을 검증한다.

## Execution Result

- OrbStack Kubernetes control plane을 기동했다.
- 두 HPA를 local runtime에서만 2/2로 제한한 뒤, 여섯 애플리케이션 Deployment가 모두 Ready가 됐다.
- 만료된 local TLS Secret을 갱신한 뒤 `ROLLOUT_TIMEOUT=600s`로 gate를 실행했다.
- gate가 rollout, HTTPS health, HTTP 308 redirect, NetworkPolicy 허용·차단을 모두 통과했다.
- HPA는 원래 min 2, max 10으로 복원했다.
