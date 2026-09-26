#!/usr/bin/env bash
# 실제 Kubernetes 클러스터의 출시 전 네트워크와 Ingress 증적을 fail-closed로 검증하는 스크립트

set -euo pipefail

readonly NAMESPACE="${NAMESPACE:-notification-hub}"
readonly INGRESS_HOST="${INGRESS_HOST:-notification-hub.local}"
readonly TLS_SECRET="${TLS_SECRET:-notification-hub-ingress-tls}"
readonly ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-180s}"
readonly DEBUG_TTL="${DEBUG_TTL:-300}"
readonly INGRESS_ADDRESS="${INGRESS_ADDRESS:-}"
readonly INGRESS_CA_CERT="${INGRESS_CA_CERT:-}"
readonly GATEWAY_DEBUG_POD="release-gate-gateway-debug"
readonly UNTRUSTED_DEBUG_POD="release-gate-untrusted-debug"

services=(discovery-service api-gateway user-service notification-service delivery-service analytics-service)
internal_services=(user-service:8081 notification-service:8082 delivery-service:8083 analytics-service:8084)

usage() {
  cat <<'EOF'
Usage: live-release-gate.sh

Required environment:
  INGRESS_ADDRESS  Ingress controller external IPv4 address or DNS-resolved address

Optional environment:
  INGRESS_CA_CERT  CA bundle for a private or test certificate
  NAMESPACE        Kubernetes namespace, default: notification-hub
  INGRESS_HOST     Ingress host, default: notification-hub.local
  TLS_SECRET       Ingress TLS Secret, default: notification-hub-ingress-tls
  ROLLOUT_TIMEOUT  kubectl rollout timeout, default: 180s
  DEBUG_TTL        Debug pod lifetime in seconds, default: 300
EOF
}

while (($# > 0)); do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

command -v kubectl >/dev/null 2>&1 || { echo "kubectl is required" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "curl is required" >&2; exit 1; }
: "${INGRESS_ADDRESS:?INGRESS_ADDRESS is required}"

cleanup() {
  kubectl delete pod "$GATEWAY_DEBUG_POD" "$UNTRUSTED_DEBUG_POD" \
    -n "$NAMESPACE" --ignore-not-found --wait=false >/dev/null 2>&1 || true
}
trap cleanup EXIT

kubectl cluster-info >/dev/null
kubectl get namespace "$NAMESPACE" >/dev/null

for service in "${services[@]}"; do
  kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout="$ROLLOUT_TIMEOUT"
done

actual_tls_secret=$(kubectl get ingress api-gateway-ingress -n "$NAMESPACE" \
  -o jsonpath='{.spec.tls[0].secretName}')
[[ "$actual_tls_secret" == "$TLS_SECRET" ]] || {
  echo "Ingress TLS Secret mismatch: expected=$TLS_SECRET actual=$actual_tls_secret" >&2
  exit 1
}
kubectl get secret "$TLS_SECRET" -n "$NAMESPACE" -o jsonpath='{.type}' | grep -Fx 'kubernetes.io/tls' >/dev/null

curl_args=(--fail --silent --show-error --resolve "$INGRESS_HOST:443:$INGRESS_ADDRESS")
if [[ -n "$INGRESS_CA_CERT" ]]; then
  curl_args+=(--cacert "$INGRESS_CA_CERT")
fi
curl "${curl_args[@]}" "https://$INGRESS_HOST/actuator/health" >/dev/null

http_status=$(curl --silent --show-error --head --output /dev/null --write-out '%{http_code}' \
  --resolve "$INGRESS_HOST:80:$INGRESS_ADDRESS" "http://$INGRESS_HOST/actuator/health")
[[ "$http_status" == 301 || "$http_status" == 308 ]] || {
  echo "Expected HTTP to HTTPS redirect, received status=$http_status" >&2
  exit 1
}

kubectl run "$GATEWAY_DEBUG_POD" -n "$NAMESPACE" --image=curlimages/curl:8.10.1 \
  --restart=Never --labels="app=api-gateway,release-gate=true" \
  --command -- sleep "$DEBUG_TTL" >/dev/null
kubectl run "$UNTRUSTED_DEBUG_POD" -n "$NAMESPACE" --image=curlimages/curl:8.10.1 \
  --restart=Never --labels="release-gate=true" \
  --command -- sleep "$DEBUG_TTL" >/dev/null
kubectl wait --for=condition=Ready pod/"$GATEWAY_DEBUG_POD" pod/"$UNTRUSTED_DEBUG_POD" \
  -n "$NAMESPACE" --timeout=60s

for service_port in "${internal_services[@]}"; do
  service="${service_port%%:*}"
  port="${service_port##*:}"
  kubectl exec -n "$NAMESPACE" "$GATEWAY_DEBUG_POD" -- \
    curl --fail --connect-timeout 5 "http://$service:$port/actuator/health" >/dev/null
  if kubectl exec -n "$NAMESPACE" "$UNTRUSTED_DEBUG_POD" -- \
    curl --fail --connect-timeout 5 "http://$service:$port/actuator/health" >/dev/null 2>&1; then
    echo "NetworkPolicy unexpectedly allowed untrusted access: $service:$port" >&2
    exit 1
  fi
done

cat <<'EOF'
Live Kubernetes release gate passed.
Rollback and separate-environment backup restore evidence remain manual release requirements.
EOF
