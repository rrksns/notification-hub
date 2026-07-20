#!/usr/bin/env bash
# Android FCM 실제 발송 검증 전 환경변수 준비 상태를 확인하는 스크립트
set -euo pipefail

env_file="${1:-.env.local}"

if [[ ! -f "$env_file" ]]; then
  echo "missing env file: $env_file"
  exit 2
fi

set -a
# shellcheck disable=SC1090
source "$env_file"
set +a

missing=()

if [[ "${PUSH_PROVIDER:-}" != "fcm" ]]; then
  missing+=("PUSH_PROVIDER=fcm")
fi

if [[ -z "${FCM_PROJECT_ID:-}" ]]; then
  missing+=("FCM_PROJECT_ID")
fi

if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS:-}" && -z "${FCM_CREDENTIALS_JSON:-}" ]]; then
  missing+=("GOOGLE_APPLICATION_CREDENTIALS or FCM_CREDENTIALS_JSON")
fi

if [[ -n "${GOOGLE_APPLICATION_CREDENTIALS:-}" && ! -f "${GOOGLE_APPLICATION_CREDENTIALS}" ]]; then
  missing+=("GOOGLE_APPLICATION_CREDENTIALS file exists")
fi

if [[ -z "${ANDROID_FCM_REGISTRATION_TOKEN:-}" ]]; then
  missing+=("ANDROID_FCM_REGISTRATION_TOKEN")
fi

if (( ${#missing[@]} > 0 )); then
  echo "Android FCM verification is not ready."
  printf 'missing: %s\n' "${missing[@]}"
  exit 2
fi

echo "Android FCM verification environment is ready."
