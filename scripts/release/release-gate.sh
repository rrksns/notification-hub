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

bash -n scripts/backup/backup.sh scripts/backup/restore.sh scripts/release/release-gate.sh
scripts/backup/backup.sh --dry-run
docker compose --env-file .env.example config --quiet

ruby -ryaml -e '
  files = Dir["k8s/**/*.yaml"].sort
  abort "no Kubernetes manifests found" if files.empty?
  files.each do |file|
    YAML.load_stream(File.read(file))
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
