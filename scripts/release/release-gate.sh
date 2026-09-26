#!/usr/bin/env bash
# 상용 배포 전 저장소 수준의 fail-closed 검증을 실행하는 스크립트

set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
skip_maven=false

usage() {
  cat <<'EOF'
Usage: release-gate.sh [--skip-maven]

Run static release-gate checks. Use --skip-maven only when CI already ran Maven verification.
EOF
}

while (($# > 0)); do
  case "$1" in
    --skip-maven)
      skip_maven=true
      shift
      ;;
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

cd "$ROOT_DIR"

command -v bash >/dev/null 2>&1 || { echo "bash is required" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
command -v ruby >/dev/null 2>&1 || { echo "ruby is required" >&2; exit 1; }

if [[ "$skip_maven" != true ]]; then
  : "${JWT_SECRET:?JWT_SECRET is required for Maven verification}"
  mvn clean verify -DskipTests=false
fi

bash -n scripts/backup/backup.sh scripts/backup/restore.sh scripts/release/release-gate.sh scripts/release/live-release-gate.sh
scripts/backup/backup.sh --dry-run
docker compose --env-file .env.example config --quiet

ruby -ryaml -e '
  files = Dir["k8s/**/*.yaml"].sort
  abort "no Kubernetes manifests found" if files.empty?
  files.each do |file|
    documents = YAML.load_stream(File.read(file))
    if file == "k8s/api-gateway/ingress.yaml"
      ingress = documents.fetch(0)
      annotations = ingress.fetch("metadata").fetch("annotations")
      tls = ingress.fetch("spec").fetch("tls")
      hosts = tls.fetch(0).fetch("hosts")
      abort "public ingress must enforce ssl redirect" unless annotations["nginx.ingress.kubernetes.io/ssl-redirect"] == "true"
      abort "public ingress must force ssl redirect" unless annotations["nginx.ingress.kubernetes.io/force-ssl-redirect"] == "true"
      abort "public ingress must declare notification-hub.local TLS" unless hosts.include?("notification-hub.local")
      abort "public ingress must use notification-hub-ingress-tls" unless tls.fetch(0).fetch("secretName") == "notification-hub-ingress-tls"
    end
    puts "Validated #{file}"
  end
'

docker run --rm --entrypoint promtool \
  -v "$PWD/monitoring/prometheus:/etc/prometheus:ro" \
  prom/prometheus:latest check rules /etc/prometheus/alerts.yml

cat <<'EOF'
Static release gate passed.
Live Kubernetes CNI, rollout, smoke, rollback, and separate-environment restore evidence remain required.
EOF
