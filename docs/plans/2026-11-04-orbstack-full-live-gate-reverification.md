# OrbStack Full Live Gate Reverification Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** OrbStack Kubernetes API와 workload가 안정된 상태에서 600초 local HPA cap live gate를 실행하고, 성공 또는 fail-closed 결과와 HPA·Deployment 복구 증적을 정확히 남긴다.

**Architecture:** 검증은 읽기 전용 preflight와 시작 상태 스냅샷으로 시작한다. `--local-hpa-cap --context orbstack`은 두 HPA 상한과 Deployment desired replica를 2로만 일시 제한하며, gate가 성공·실패·중단해도 기존 래퍼 trap이 시작 값을 복구한다. Kubernetes API 또는 readiness preflight가 실패하면 HPA를 변경하지 않고 그 사실만 기록한다.

**Tech Stack:** Bash, kubectl, OrbStack Kubernetes, HPA, NGINX Ingress NodePort, existing release gate scripts.

---

## Scope and safety boundaries

- 대상 context는 `orbstack`, namespace는 `notification-hub`으로 고정한다.
- HPA 제한은 `scripts/release/live-release-gate.sh --local-hpa-cap --context orbstack`으로만 수행한다.
- `user-service-hpa`, `notification-service-hpa`, 같은 이름 Deployment 이외의 Kubernetes 리소스를 수정하지 않는다.
- API server가 Ready가 아니거나 여섯 서비스가 안정화 조건을 만족하지 않으면 full gate를 시작하지 않는다.
- 이 계획은 상용 또는 원격 cluster 실행, rollback 수행, backup restore, application 코드 수정 범위를 포함하지 않는다.

## Success criteria

1. Preflight가 OrbStack API server, node, namespace, Ingress TLS Secret, NodePort를 모두 확인한다.
2. full gate는 `ROLLOUT_TIMEOUT=600s`로 실행되며 rollout, HTTPS, HTTP redirect, Gateway 허용, untrusted Pod 차단을 모두 통과한다.
3. gate 종료 직후 두 HPA `maxReplicas`와 두 Deployment `spec.replicas`가 시작 값과 같음을 확인한다.
4. 성공 또는 실패 증적은 추측 없이 `checklist.md`와 `context-notes.md`에 기록한다.

### Task 1: 로컬 gate 계약의 기준선을 확인한다.

**Files:**
- Test: `scripts/test/live-release-gate-test.sh`
- Test: `scripts/test/with-local-hpa-cap-test.sh`

**Step 1: fake command 회귀 테스트를 실행한다.**

Run:

```bash
bash scripts/test/live-release-gate-test.sh
bash scripts/test/with-local-hpa-cap-test.sh
bash -n scripts/release/live-release-gate.sh scripts/with-local-hpa-cap.sh \
  scripts/test/live-release-gate-test.sh scripts/test/with-local-hpa-cap-test.sh
```

Expected: 두 테스트가 통과하고, `--local-hpa-cap`의 context 필수·단일 래핑·복구 계약이 유지된다.

**Step 2: 실패하면 실제 cluster 실행을 시작하지 않는다.**

실패한 테스트와 출력을 기록하고, 해당 스크립트 계약을 고치는 별도 작업으로 분리한다.

### Task 2: Kubernetes API와 시작 상태를 변경 없이 확인한다.

**Files:**
- Modify after execution: `checklist.md`
- Modify after execution: `context-notes.md`

**Step 1: API server와 cluster 접근성을 확인한다.**

Run:

```bash
kubectl --context orbstack get --raw='/readyz?verbose'
kubectl --context orbstack get nodes -o wide
kubectl --context orbstack get namespace notification-hub
```

Expected: readyz, node, namespace가 모두 성공한다.

**Step 2: gate 입력과 시작 값을 스냅샷한다.**

Run:

```bash
kubectl --context orbstack get hpa user-service-hpa notification-service-hpa \
  -n notification-hub \
  -o custom-columns=NAME:.metadata.name,MIN:.spec.minReplicas,MAX:.spec.maxReplicas,CURRENT:.status.currentReplicas,DESIRED:.status.desiredReplicas
kubectl --context orbstack get deployment user-service notification-service \
  -n notification-hub \
  -o custom-columns=NAME:.metadata.name,DESIRED:.spec.replicas,AVAILABLE:.status.availableReplicas,READY:.status.readyReplicas
kubectl --context orbstack get ingress api-gateway-ingress -n notification-hub \
  -o jsonpath='{.spec.tls[0].secretName}{"\n"}'
kubectl --context orbstack get secret notification-hub-ingress-tls -n notification-hub \
  -o jsonpath='{.type}{"\n"}'
kubectl --context orbstack get service -n ingress-nginx ingress-nginx-controller
kubectl --context orbstack get pods -n notification-hub -o wide
kubectl --context orbstack get events -n notification-hub --sort-by=.lastTimestamp | tail -50
```

Expected: TLS Secret은 `notification-hub-ingress-tls`, type은 `kubernetes.io/tls`, NodePort는 HTTP `31193`와 HTTPS `31856`이다. 시작 HPA·Deployment 값과 Pod readiness를 기록한다.

**Step 3: API preflight가 실패하면 중단한다.**

`apiserver not ready` 또는 `ServiceUnavailable`이면 HPA를 제한하지 않는다. control plane 비가용 시각과 출력만 `context-notes.md`에 기록하고, OrbStack 안정화 후 Task 2부터 재개한다.

### Task 3: 제한 후 workload readiness를 분리 진단한다.

**Files:**
- Modify after execution: `context-notes.md`

**Step 1: local HPA cap 상태에서 여섯 Deployment rollout을 확인한다.**

Run:

```bash
scripts/with-local-hpa-cap.sh --context orbstack -- \
  bash -c '
    for service in discovery-service api-gateway user-service notification-service delivery-service analytics-service; do
      kubectl --context orbstack rollout status deployment/"$service" -n notification-hub --timeout=600s || exit 1
    done
  '
```

Expected: 모든 Deployment가 600초 안에 Ready가 된다. 래퍼 종료 뒤 HPA max와 Deployment desired replica가 Task 2 시작 값으로 복구된다.

**Step 2: readiness 실패 시 원인을 한 가지 증거 묶음으로 수집한다.**

Run:

```bash
kubectl --context orbstack get pods -n notification-hub -o wide
kubectl --context orbstack get events -n notification-hub --sort-by=.lastTimestamp | tail -100
kubectl --context orbstack describe deployment discovery-service -n notification-hub
kubectl --context orbstack logs deployment/discovery-service -n notification-hub --tail=200
```

Expected: 실패 Deployment, readiness probe, restart reason, 의존성 연결 오류 중 실제 원인을 식별할 수 있는 출력이 남는다.

**Step 3: 원인이 확인되기 전에는 full gate를 실행하지 않는다.**

readiness 실패는 HPA 래퍼 결함으로 기록하지 않는다. application 또는 cluster 원인에 대한 별도 수정 계획을 만들고, 수정 후 Task 1부터 다시 검증한다.

### Task 4: 600초 full local live gate를 실행한다.

**Files:**
- Modify after execution: `checklist.md`
- Modify after execution: `context-notes.md`
- Modify after execution: `docs/operations/release-gate.md`

**Step 1: TLS CA bundle을 memory stream으로 전달해 명령을 실행한다.**

Run:

```bash
INGRESS_ADDRESS=127.0.0.1 \
INGRESS_HTTP_PORT=31193 \
INGRESS_HTTPS_PORT=31856 \
INGRESS_CA_CERT=<(kubectl --context orbstack get secret notification-hub-ingress-tls \
  -n notification-hub -o jsonpath='{.data.tls\.crt}' | base64 --decode) \
ROLLOUT_TIMEOUT=600s \
bash scripts/release/live-release-gate.sh --local-hpa-cap --context orbstack
```

Expected: 여섯 rollout, TLS Secret type, HTTPS actuator health, HTTP 301 또는 308 redirect, Gateway debug Pod 허용, untrusted debug Pod 차단이 모두 통과한다.

**Step 2: 실패 시 단계와 종료 코드를 기록한다.**

실패 뒤에도 HPA·Deployment 복구를 확인한다. 첫 실패 단계가 rollout이면 Task 3으로 돌아가고, TLS·redirect·NetworkPolicy 단계 실패면 해당 계약만 별도 진단한다.

### Task 5: 복구와 증적을 확인하고 문서화한다.

**Files:**
- Modify: `checklist.md`
- Modify: `context-notes.md`
- Modify: `docs/operations/release-gate.md`

**Step 1: 시작 값과 종료 값을 비교한다.**

Run:

```bash
kubectl --context orbstack get hpa user-service-hpa notification-service-hpa \
  -n notification-hub \
  -o custom-columns=NAME:.metadata.name,MIN:.spec.minReplicas,MAX:.spec.maxReplicas,CURRENT:.status.currentReplicas,DESIRED:.status.desiredReplicas
kubectl --context orbstack get deployment user-service notification-service \
  -n notification-hub \
  -o custom-columns=NAME:.metadata.name,DESIRED:.spec.replicas,AVAILABLE:.status.availableReplicas,READY:.status.readyReplicas
kubectl --context orbstack get pod release-gate-gateway-debug release-gate-untrusted-debug \
  -n notification-hub --ignore-not-found
```

Expected: `maxReplicas`와 desired replica가 Task 2의 시작 값으로 복구되고, debug Pod는 삭제됐거나 terminating 상태다.

**Step 2: 성공과 실패를 구분해 기록한다.**

- 성공이면 full live gate 항목을 통과로 표시하고 실행 시각, context, 시작·종료 HPA 값, 모든 검증 단계를 기록한다.
- 실패이면 실패 단계, 종료 코드, 복구 결과, 재검증 전제만 기록한다. 통과하지 않은 TLS·HTTP·NetworkPolicy 검증을 완료로 표시하지 않는다.

**Step 3: 문서 변경을 검증하고 별도 커밋한다.**

Run:

```bash
git diff --check
git add checklist.md context-notes.md docs/operations/release-gate.md
git commit -m "문서: OrbStack full live gate 재검증 기록"
```

Expected: 실행 증적 문서만 포함한 별도 커밋이 생성된다.

## Manual QA evidence

- API server readyz와 node 상태 출력.
- 시작·종료 HPA max와 Deployment desired replica 비교.
- 여섯 Deployment readiness와 최근 Kubernetes event 출력.
- full gate의 종료 코드와 rollout, TLS, HTTPS, redirect, 허용·차단 결과.
- gate 종료 뒤 debug Pod cleanup 결과.
