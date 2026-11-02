#!/usr/bin/env bash
# live release gate의 로컬 HPA 제한 opt-in과 context 전달 계약을 가짜 명령으로 검증한다.

set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly GATE="$ROOT_DIR/scripts/release/live-release-gate.sh"

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
  grep -F -- "$expected" "$KUBECTL_LOG" >/dev/null || {
    cat "$KUBECTL_LOG" >&2
    fail "missing kubectl call: $expected"
  }
}

assert_not_contains() {
  local unexpected=$1
  if grep -F -- "$unexpected" "$KUBECTL_LOG" >/dev/null; then
    cat "$KUBECTL_LOG" >&2
    fail "unexpected kubectl call: $unexpected"
  fi
}

reset_fixture() {
  [[ -z "$workspace" ]] || rm -rf "$workspace"
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
    get|patch|scale|cluster-info|rollout|run|wait|exec|delete)
      command="${arguments[$index]}"
      command_index=$index
      break
      ;;
  esac
done

case "${command:-}" in
  exec)
    [[ " $* " == *" release-gate-untrusted-debug "* ]] && exit 1
    exit 0
    ;;
  cluster-info|rollout|run|wait|delete|patch|scale)
    exit 0
    ;;
  get)
    resource="${arguments[$((command_index + 1))]:-}"
    name="${arguments[$((command_index + 2))]:-}"
    case "$resource:$name" in
      namespace:notification-hub)
        exit 0
        ;;
      hpa:user-service-hpa|hpa:notification-service-hpa)
        printf '10'
        ;;
      deployment:user-service|deployment:notification-service)
        printf '10'
        ;;
      ingress:api-gateway-ingress)
        printf 'notification-hub-ingress-tls'
        ;;
      secret:notification-hub-ingress-tls)
        printf 'kubernetes.io/tls'
        ;;
      *)
        echo "unexpected get: $resource $name" >&2
        exit 1
        ;;
    esac
    ;;
  *)
    echo "unexpected kubectl call: $*" >&2
    exit 1
    ;;
esac
EOF

  cat > "$workspace/bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

if [[ " $* " == *" --head "* ]]; then
  printf '308'
fi
EOF

  chmod +x "$workspace/bin/kubectl" "$workspace/bin/curl"
}

run_gate() {
  INGRESS_ADDRESS=127.0.0.1 \
    INGRESS_HTTP_PORT=31193 \
    INGRESS_HTTPS_PORT=31856 \
    "$GATE" "$@"
}

run_default_gate_does_not_limit_hpa() {
  reset_fixture

  run_gate || fail "default gate failed"

  assert_not_contains 'patch hpa user-service-hpa'
  assert_not_contains 'scale deployment user-service'
}

run_context_gate_uses_the_requested_context() {
  reset_fixture

  run_gate --context fake || fail "context gate failed"

  assert_contains '--context fake cluster-info'
  assert_contains '--context fake rollout status deployment/user-service'
  assert_contains '--context fake delete pod release-gate-gateway-debug release-gate-untrusted-debug'
}

run_local_cap_without_context_fails_before_cluster_changes() {
  reset_fixture

  local status=0
  run_gate --local-hpa-cap || status=$?
  [[ "$status" == 2 ]] || fail "missing local-cap context returned status=$status"

  assert_not_contains 'patch hpa user-service-hpa'
  assert_not_contains 'scale deployment user-service'
}

run_local_cap_wraps_the_gate_once() {
  reset_fixture

  run_gate --local-hpa-cap --context fake || fail "local-cap gate failed"

  assert_contains '--context fake patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":2}}'
  assert_contains '--context fake scale deployment notification-service -n notification-hub --replicas=2'
  assert_contains '--context fake patch hpa user-service-hpa -n notification-hub --type merge -p {"spec":{"maxReplicas":10}}'
  assert_contains '--context fake rollout status deployment/user-service'
}

run_active_inner_gate_does_not_wrap_again() {
  reset_fixture

  LOCAL_HPA_CAP_ACTIVE=true run_gate --local-hpa-cap --context fake || fail "active inner gate failed"

  assert_not_contains 'patch hpa user-service-hpa'
  assert_not_contains 'scale deployment user-service'
  assert_contains '--context fake rollout status deployment/user-service'
}

run_default_gate_does_not_limit_hpa
run_context_gate_uses_the_requested_context
run_local_cap_without_context_fails_before_cluster_changes
run_local_cap_wraps_the_gate_once
run_active_inner_gate_does_not_wrap_again

echo "live-release-gate tests passed"
