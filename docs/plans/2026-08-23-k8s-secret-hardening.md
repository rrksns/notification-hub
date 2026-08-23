# K8s Secret Hardening Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** K8s secret 값을 Git에서 제거하고 배포 시 클러스터 Secret으로 주입한다.

**Architecture:** Git에는 `k8s/secret.example.yaml`만 남긴다. 실제 `k8s/secret.yaml`은 ignore하고, README는 `kubectl create secret generic ... --dry-run=client -o yaml | kubectl apply -f -` 절차를 안내한다. 앱과 infra Pod는 기존 `notification-hub-secret` 이름을 계속 참조한다.

**Tech Stack:** Kubernetes Secret, kubectl, YAML, Markdown.

---

### Task 1: Secret manifest 정리

**Files:**
- Delete: `k8s/secret.yaml`
- Create: `k8s/secret.example.yaml`
- Modify: `.gitignore`

**Step 1: Remove tracked real Secret**

`k8s/secret.yaml`을 삭제하고 `.gitignore`에 `k8s/secret.yaml`을 추가한다.

**Step 2: Add example Secret**

`k8s/secret.example.yaml`에는 `MYSQL_ROOT_PASSWORD`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `MONGODB_USERNAME`, `MONGODB_PASSWORD`, `JWT_SECRET` 키와 placeholder만 둔다.

### Task 2: Infra manifest secretKeyRef 적용

**Files:**
- Modify: `k8s/infra/mysql.yaml`
- Modify: `k8s/infra/mongodb.yaml`

**Step 1: Update MySQL env**

MySQL root/user/password 값을 `notification-hub-secret`의 `MYSQL_ROOT_PASSWORD`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`에서 읽게 한다.

**Step 2: Update MongoDB env**

MongoDB root username/password 값을 `notification-hub-secret`의 `MONGODB_USERNAME`, `MONGODB_PASSWORD`에서 읽게 한다.

### Task 3: Documentation update

**Files:**
- Modify: `README.md`
- Modify: `PROCESS.md`
- Modify: `manual_test.md`
- Modify: `docs/improvement-todo.md`
- Modify: `docs/plans/2026-08-19-commercialization-priority-list.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: Document Secret creation**

README의 K8s 배포 단계에서 `k8s/secret.yaml` apply 대신 `kubectl create secret generic ... --dry-run=client -o yaml | kubectl apply -f -`를 사용한다.

**Step 2: Mark priority item**

상용화 우선순위 2번과 개선 문서에 Git에 평문 Secret을 남기지 않는다고 기록한다.

### Task 4: Verification and commit

**Step 1: Search for old secret values**

Run: `rg -n "nhub1234|root1234|bm90aWZpY2F0aW9uaHViLXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5n" k8s --glob '!k8s/secret.example.yaml'`

Expected: no active K8s manifest contains the removed password or JWT secret values.

**Step 2: Validate YAML parse**

Run a local YAML parser over `k8s/**/*.yaml`.

Expected: all active K8s YAML files parse.

**Step 3: Verify kubectl secret generation**

Run: `kubectl create secret generic notification-hub-secret -n notification-hub --from-literal=... --dry-run=client -o yaml`

Expected: Secret YAML is generated locally.

**Step 4: Commit**

Run: `git commit -m "feat: K8s Secret 평문 제거"`
