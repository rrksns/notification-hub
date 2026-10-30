#!/usr/bin/env bash
# 로컬 Kubernetes 검증 중 두 HPA 상한을 임시 제한하고 원래 값으로 복구한다.

set -euo pipefail

readonly CAP_REPLICAS=2
namespace="notification-hub"
context=""
command=()
hpas=(user-service-hpa notification-service-hpa)
deployments=(user-service notification-service)
hpa_maxes=()
deployment_replicas=()
hpa_changed=(false false)
deployment_changed=(false false)
cleanup_done=false
child_pid=""

usage() {
  cat <<'EOF'
Usage: with-local-hpa-cap.sh --context NAME [--namespace NAME] -- COMMAND [ARG...]

Temporarily cap notification-hub user-service and notification-service HPAs at two replicas.
EOF
}

fail() {
  echo "$*" >&2
  exit 2
}

cleanup() {
  local exit_status=$?
  local index
  local restore_failed=false

  [[ "$cleanup_done" == true ]] && return "$exit_status"
  cleanup_done=true

  for index in "${!hpas[@]}"; do
    if [[ "${hpa_changed[$index]}" == true ]]; then
      kubectl --context "$context" patch hpa "${hpas[$index]}" -n "$namespace" \
        --type merge -p "{\"spec\":{\"maxReplicas\":${hpa_maxes[$index]}}}" || restore_failed=true
    fi
    if [[ "${deployment_changed[$index]}" == true ]]; then
      kubectl --context "$context" scale deployment "${deployments[$index]}" -n "$namespace" \
        --replicas="${deployment_replicas[$index]}" || restore_failed=true
    fi
  done

  if [[ "$restore_failed" == true ]]; then
    echo "failed to restore local HPA limits" >&2
    return 1
  fi
  return "$exit_status"
}

on_signal() {
  local signal=$1
  local status=$2
  if [[ -n "$child_pid" ]]; then
    kill -"$signal" "$child_pid" 2>/dev/null || true
  fi
  exit "$status"
}

while (($# > 0)); do
  case "$1" in
    --context)
      (($# >= 2)) || fail "--context requires a value"
      context=$2
      shift 2
      ;;
    --namespace)
      (($# >= 2)) || fail "--namespace requires a value"
      namespace=$2
      shift 2
      ;;
    --)
      shift
      command=("$@")
      break
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      fail "unknown option: $1"
      ;;
  esac
done

[[ -n "$context" ]] || fail "--context is required"
((${#command[@]} > 0)) || fail "verification command is required after --"
command -v kubectl >/dev/null 2>&1 || { echo "kubectl is required" >&2; exit 1; }

kubectl --context "$context" cluster-info >/dev/null
kubectl --context "$context" get namespace "$namespace" >/dev/null

for index in "${!hpas[@]}"; do
  hpa_maxes[$index]=$(kubectl --context "$context" get hpa "${hpas[$index]}" -n "$namespace" \
    -o jsonpath='{.spec.maxReplicas}')
  deployment_replicas[$index]=$(kubectl --context "$context" get deployment "${deployments[$index]}" -n "$namespace" \
    -o jsonpath='{.spec.replicas}')
  [[ "${hpa_maxes[$index]}" =~ ^[0-9]+$ ]] || { echo "invalid HPA maximum" >&2; exit 1; }
  [[ "${deployment_replicas[$index]}" =~ ^[0-9]+$ ]] || { echo "invalid Deployment replica count" >&2; exit 1; }
done

trap cleanup EXIT
trap 'on_signal INT 130' INT
trap 'on_signal TERM 143' TERM

for index in "${!hpas[@]}"; do
  if (( hpa_maxes[index] > CAP_REPLICAS )); then
    kubectl --context "$context" patch hpa "${hpas[$index]}" -n "$namespace" \
      --type merge -p "{\"spec\":{\"maxReplicas\":$CAP_REPLICAS}}"
    hpa_changed[$index]=true
  fi
  if (( deployment_replicas[index] > CAP_REPLICAS )); then
    kubectl --context "$context" scale deployment "${deployments[$index]}" -n "$namespace" \
      --replicas="$CAP_REPLICAS"
    deployment_changed[$index]=true
  fi
done

"${command[@]}" &
child_pid=$!
wait "$child_pid"
