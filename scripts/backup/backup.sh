#!/usr/bin/env bash
# Notification Hub Compose 데이터베이스와 메시징 메타데이터를 백업하는 스크립트

set -euo pipefail

readonly DEFAULT_BACKUP_ROOT="backups/notification-hub"
dry_run=false
backup_root="${BACKUP_ROOT:-$DEFAULT_BACKUP_ROOT}"

usage() {
  cat <<'EOF'
Usage: backup.sh [--output DIR] [--dry-run]

Create a timestamped backup directory for the Compose data services.
EOF
}

while (($# > 0)); do
  case "$1" in
    --output)
      [[ $# -ge 2 ]] || { echo "--output requires a directory" >&2; exit 2; }
      backup_root=$2
      shift 2
      ;;
    --dry-run)
      dry_run=true
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

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required" >&2
  exit 1
fi

if [[ "$dry_run" == true ]]; then
  cat <<EOF
DRY-RUN backup root: $backup_root
Would run: docker compose exec -T mysql mysqldump --all-databases
Would run: docker compose exec -T mongodb mongodump --archive --gzip
Would run: docker compose exec -T redis redis-cli BGSAVE and copy /data/dump.rdb
Would run: docker compose exec -T kafka kafka-topics.sh --list and --describe
EOF
  exit 0
fi

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_dir="$backup_root/$timestamp"
mkdir -p "$backup_dir/mysql" "$backup_dir/mongodb" "$backup_dir/redis" "$backup_dir/kafka"

docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases --single-transaction --routines --events' \
  > "$backup_dir/mysql/all-databases.sql"
docker compose exec -T mongodb sh -c 'mongodump --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --db analytics --archive --gzip' \
  > "$backup_dir/mongodb/analytics.archive.gz"

docker compose exec -T redis redis-cli BGSAVE >/dev/null
for _ in {1..60}; do
  if [[ "$(docker compose exec -T redis redis-cli INFO persistence | tr -d '\r' | awk -F: '/rdb_bgsave_in_progress/{print $2}')" == "0" ]]; then
    break
  fi
  sleep 1
done
if [[ "$(docker compose exec -T redis redis-cli INFO persistence | tr -d '\r' | awk -F: '/rdb_bgsave_in_progress/{print $2}')" != "0" ]]; then
  echo "Redis BGSAVE did not complete within 60 seconds" >&2
  exit 1
fi
docker cp notification-hub-redis:/data/dump.rdb "$backup_dir/redis/dump.rdb"

docker compose exec -T kafka bash -c '
  set -e
  topics=$(/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list)
  printf "%s\n" "$topics" > /tmp/notification-hub-topics.txt
  : > /tmp/notification-hub-topic-specs.txt
  : > /tmp/notification-hub-topic-configs.txt
  while IFS= read -r topic; do
    [ -n "$topic" ] || continue
    partitions=$(/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic "$topic" | sed -n "1s/.*PartitionCount: \([0-9][0-9]*\).*/\1/p")
    printf "%s\t%s\n" "$topic" "${partitions:-1}" >> /tmp/notification-hub-topic-specs.txt
    /opt/kafka/bin/kafka-configs.sh --bootstrap-server kafka:9092 --entity-type topics --entity-name "$topic" --describe >> /tmp/notification-hub-topic-configs.txt || true
  done < /tmp/notification-hub-topics.txt
'
docker cp notification-hub-kafka:/tmp/notification-hub-topics.txt "$backup_dir/kafka/topics.txt"
docker cp notification-hub-kafka:/tmp/notification-hub-topic-specs.txt "$backup_dir/kafka/topic-specs.txt"
docker cp notification-hub-kafka:/tmp/notification-hub-topic-configs.txt "$backup_dir/kafka/topic-configs.txt"

cat > "$backup_dir/manifest.txt" <<EOF
created_at_utc=$timestamp
mysql=mysql/all-databases.sql
mongodb=mongodb/analytics.archive.gz
redis=redis/dump.rdb
kafka_topics=kafka/topics.txt
kafka_topic_specs=kafka/topic-specs.txt
kafka_topic_configs=kafka/topic-configs.txt
EOF

printf 'Backup completed: %s\n' "$backup_dir"
