# K8s NetworkPolicy Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** api-gateway를 우회한 내부 서비스 Pod 직접 접근을 Kubernetes 네트워크 레벨에서 차단한다.

**Architecture:** `notification-hub` namespace 안에서 user, notification, delivery, analytics service Pod를 ingress-isolated 상태로 만든다. api-gateway Pod는 서비스 API 포트에 접근할 수 있고, 모니터링 Pod는 actuator health/prometheus 포트 접근만 허용한다. 애플리케이션 레벨 JWT 검증은 이미 적용되어 있으므로 이번 작업은 K8s defense-in-depth 레이어만 추가한다.

**Tech Stack:** Kubernetes `networking.k8s.io/v1` NetworkPolicy, kubectl client-side dry-run, Markdown docs.

---

### Task 1: NetworkPolicy manifest 추가

**Files:**
- Create: `k8s/networkpolicy/internal-services-ingress.yaml`

**Step 1: Add default deny policy**

`app in (user-service, notification-service, delivery-service, analytics-service)` Pod에 ingress default deny를 적용한다.

**Step 2: Add api-gateway allow policy**

`app: api-gateway` Pod에서 각 내부 서비스의 HTTP 포트로 들어오는 ingress만 허용한다.

**Step 3: Add monitoring allow policy**

`monitoring: "true"` label을 가진 namespace 또는 `app.kubernetes.io/part-of: monitoring` label을 가진 namespace의 Pod가 내부 서비스 actuator 포트에 접근할 수 있게 허용한다.

### Task 2: 문서와 체크리스트 갱신

**Files:**
- Modify: `README.md`
- Modify: `PROCESS.md`
- Modify: `docs/improvement-todo.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: Document NetworkPolicy scope**

내부 서비스는 api-gateway와 승인된 monitoring Pod의 ingress만 허용한다고 기록한다.

**Step 2: Keep P2 #25 in progress if needed**

NetworkPolicy manifest 검증이 끝나면 #25를 완료로 바꾼다. 실제 cluster CNI enforcement는 운영 환경에서 별도 확인해야 한다고 남긴다.

### Task 3: 검증과 커밋

**Step 1: Validate manifests**

Run: `kubectl apply --dry-run=client -f k8s/networkpolicy/`

Expected: client-side validation succeeds.

If the local Kubernetes API is unavailable, record the failure and run a local YAML structure check that verifies every document is a `networking.k8s.io/v1` `NetworkPolicy` with namespace, selector, ingress policy type, and allow-rule ports.

**Step 2: Validate repository diff**

Run: `git diff --check`

Expected: no whitespace errors.

**Step 3: Commit**

Run: `git commit -m "feat: 내부 서비스 NetworkPolicy 추가"`
