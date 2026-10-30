#!/usr/bin/env bash
# 로컬 HPA 제한 래퍼의 Kubernetes 변경과 복구 계약을 가짜 kubectl로 검증한다.

set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly WRAPPER="$ROOT_DIR/scripts/with-local-hpa-cap.sh"

workspace=""

cleanup() {
  [[ -z "$workspace" ]] || rm -rf "$workspace"
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

assert_contains() {
  local expected=$1
  grep -F "$expected" "$KUBECTL_LOG" >/dev/null || {
    cat "$KUBECTL_LOG" >&2
    fail "missing kubectl call: $expected"
  }
}

assert_not_contains() {
  local unexpected=$1
  if grep -F "$unexpected" "$KUBECTL_LOG" >/dev/null; then
    cat "$KUBECTL_LOG" >&2
    fail "unexpected kubectl call: $unexpected"
  fi
}

reset_fixture() {
  workspace=$(mktemp -d)
  export KUBECTL_LOG="$workspace/kubectl.log"
  export PATH="$workspace/bin:$PATH"
  mkdir -p "$workspace/bin"
  : > "$KUBECTL_LOG"

  cat > "$workspace/bin/kubectl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >> "$KUBECTL_LOG"

arguments=("$@")
for index in "${!arguments[@]}"; do
  case "${arguments[$index]}" in
    get|patch|scale|cluster-info)
      command="${arguments[$index]}"
      command_index=$index
      break
      ;;
  esac
done

case "${command:-}" in
  cluster-info)
    exit 0
    ;;
  get)
    resource="${arguments[$((command_index + 1))]:-}"
    name="${arguments[$((command_index + 2))]:-}"
    if [[ "$resource" == namespace ]]; then
      exit 0
    fi
    case "$resource:$name" in
      hpa:user-service-hpa|hpa:notification-service-hpa)
        printf '%s' "${FAKE_HPA_MAX:-10}"
        ;;
      deployment:user-service|deployment:notification-service)
        printf '%s' "${FAKE_DEPLOYMENT_REPLICAS:-10}"
        ;;
      *)
        echo "unexpected get: $resource $name" >&2
        exit 1
        ;;
    esac
    ;;
  patch|scale)
    exit 0
    ;;
  *)
    echo "unexpected kubectl call: $*" >&2
    exit 1
    ;;
esac
EOF
  chmod +x "$workspace/bin/kubectl"
}

run_success_restores_original_values() {
  reset_fixture

  "$WRAPPER" --context fake -- true || fail "successful verification command failed"

  assert_contains 'patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":2}}'
  assert_contains 'patch hpa notification-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":2}}'
  assert_contains 'scale deployment user-service -n notification-hub --replicas=2'
  assert_contains 'scale deployment notification-service -n notification-hub --replicas=2'
  assert_contains 'patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":10}}'
  assert_contains 'patch hpa notification-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":10}}'
  assert_contains 'scale deployment user-service -n notification-hub --replicas=10'
  assert_contains 'scale deployment notification-service -n notification-hub --replicas=10'
}

run_failure_restores_original_values() {
  reset_fixture

  if "$WRAPPER" --context fake -- false; then
    fail "failed verification command returned success"
  fi

  assert_contains 'patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":10}}'
  assert_contains 'scale deployment notification-service -n notification-hub --replicas=10'
}

run_invalid_input_does_not_modify_cluster() {
  reset_fixture

  if "$WRAPPER" -- true; then
    fail "missing context returned success"
  fi
  if "$WRAPPER" --context fake; then
    fail "missing verification command returned success"
  fi

  assert_not_contains 'patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":2}}'
  assert_not_contains 'scale deployment user-service -n notification-hub --replicas=2'
}

run_already_capped_values_are_not_modified() {
  reset_fixture

  FAKE_HPA_MAX=2 FAKE_DEPLOYMENT_REPLICAS=2 "$WRAPPER" --context fake -- true || fail "already-capped verification failed"

  assert_not_contains 'patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":2}}'
  assert_not_contains 'scale deployment user-service -n notification-hub --replicas=2'
}

wait_for_call() {
  local expected=$1
  local attempt
  for attempt in {1..50}; do
    grep -F "$expected" "$KUBECTL_LOG" >/dev/null && return 0
    sleep 0.1
  done
  cat "$KUBECTL_LOG" >&2
  fail "timed out waiting for kubectl call: $expected"
}

run_term_restores_original_values() {
  reset_fixture

  "$WRAPPER" --context fake -- sh -c 'while :; do sleep 1; done' &
  local wrapper_pid=$!
  wait_for_call 'scale deployment notification-service -n notification-hub --replicas=2'
  kill -TERM "$wrapper_pid"
  wait "$wrapper_pid" || true

  assert_contains 'patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":10}}'
  assert_contains 'scale deployment notification-service -n notification-hub --replicas=10'
}

run_success_restores_original_values
run_failure_restores_original_values
run_invalid_input_does_not_modify_cluster
run_already_capped_values_are_not_modified
run_term_restores_original_values

echo "with-local-hpa-cap tests passed"
