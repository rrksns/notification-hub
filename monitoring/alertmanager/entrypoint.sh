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
    *"'"*|*'"'*|*[[:space:]]*|*'#'*|*'$'*|*'&'*|*'\\'*|*'|'*)
      error "$name contains characters that cannot be safely rendered into the Alertmanager YAML configuration"
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

config_file=$(mktemp)
trap 'rm -f "$config_file"' EXIT INT TERM

sed \
  -e "s|\${ALERTMANAGER_WEBHOOK_URL}|$ALERTMANAGER_WEBHOOK_URL|g" \
  -e "s|\${SMTP_SMARTHOST}|$SMTP_SMARTHOST|g" \
  -e "s|\${SMTP_FROM}|$SMTP_FROM|g" \
  -e "s|\${SMTP_AUTH_USERNAME}|$SMTP_AUTH_USERNAME|g" \
  -e "s|\${SMTP_AUTH_PASSWORD_FILE}|$password_file|g" \
  -e "s|\${ALERT_EMAIL_TO}|$ALERT_EMAIL_TO|g" \
  /etc/alertmanager/alertmanager.yml > "$config_file"

if grep -Fq '${' "$config_file"; then
  error 'rendered Alertmanager configuration contains an unresolved environment placeholder'
fi

exec /bin/alertmanager --config.file="$config_file"
