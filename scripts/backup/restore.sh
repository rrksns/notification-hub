#!/usr/bin/env bash
# Notification Hub 백업 산출물을 검증하고 선택적으로 복구하는 스크립트

set -euo pipefail

input_dir=""
confirm=false

usage() {
  cat <<'EOF'
Usage: restore.sh --input DIR [--confirm]

Validate a backup directory and print restore actions by default.
Pass --confirm to write to the Compose services.
EOF
}

while (($# > 0)); do
  case "$1" in
    --input)
      [[ $# -ge 2 ]] || { echo "--input requires a directory" >&2; exit 2; }
      input_dir=$2
      shift 2
      ;;
    --confirm)
      confirm=true
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

if [[ -z "$input_dir" ]]; then
  echo "--input is required" >&2
  exit 2
fi

required_files=(
  "$input_dir/mysql/all-databases.sql"
  "$input_dir/mongodb/analytics.archive.gz"
  "$input_dir/redis/dump.rdb"
  "$input_dir/kafka/topic-specs.txt"
  "$input_dir/kafka/topic-configs.txt"
  "$input_dir/manifest.txt"
)
for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing backup artifact: $file" >&2; exit 1; }
done

cat <<EOF
Backup directory: $input_dir
MySQL: all databases
MongoDB: analytics database
Redis: /data/dump.rdb replacement and container restart
Kafka: recreate topics from topic-specs.txt; review topic-configs.txt before production restore
EOF

if [[ "$confirm" != true ]]; then
  echo "DRY-RUN: no services were modified. Pass --confirm to execute restore."
  exit 0
fi

command -v docker >/dev/null 2>&1 || { echo "docker is required" >&2; exit 1; }
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < "$input_dir/mysql/all-databases.sql"
docker compose exec -T mongodb sh -c 'mongorestore --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --archive --gzip --drop' < "$input_dir/mongodb/analytics.archive.gz"

docker compose stop redis >/dev/null
docker cp "$input_dir/redis/dump.rdb" notification-hub-redis:/data/dump.rdb
docker compose start redis >/dev/null

while IFS=$'\t' read -r topic partitions; do
  [[ -n "$topic" ]] || continue
  [[ "$topic" == __* ]] && continue
  docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server kafka:9092 --create --if-not-exists \
    --topic "$topic" --partitions "${partitions:-1}" --replication-factor 1 >/dev/null
done < "$input_dir/kafka/topic-specs.txt"

echo "Restore completed. Review topic-configs.txt and verify application health."
