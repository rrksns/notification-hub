#!/bin/sh
# Alertmanager 실행 전 환경변수와 비밀번호 파일을 검증하는 진입점
set -eu

error() {
  printf 'Alertmanager entrypoint error: %s\n' "$1" >&2
  exit 1
}

validate_scalar() {
  name=$1
  value=$2

  [ -n "$value" ] || error "$name must be set"

  case "$value" in
    *"'"*|*'"'*|*[[:space:]]*|*'#'*|*'$'*)
      error "$name must be a conventional unquoted YAML scalar without quotes, whitespace, comments, or dollar signs"
      ;;
  esac

  if printf '%s' "$value" | grep -Eq '(^:|:$|:[[:space:]]|:[{}\[\],])'; then
    error "$name contains an unsafe YAML colon syntax"
  fi
}

validate_scalar ALERTMANAGER_WEBHOOK_URL "${ALERTMANAGER_WEBHOOK_URL:-}"
validate_scalar SMTP_SMARTHOST "${SMTP_SMARTHOST:-}"
validate_scalar SMTP_FROM "${SMTP_FROM:-}"
validate_scalar SMTP_AUTH_USERNAME "${SMTP_AUTH_USERNAME:-}"
validate_scalar ALERT_EMAIL_TO "${ALERT_EMAIL_TO:-}"

password_file=${SMTP_AUTH_PASSWORD_FILE:-}
validate_scalar SMTP_AUTH_PASSWORD_FILE "$password_file"
[ -r "$password_file" ] || error "SMTP_AUTH_PASSWORD_FILE is not readable: $password_file"

exec /bin/alertmanager \
  --config.file=/etc/alertmanager/alertmanager.yml \
  --config.expand-env
