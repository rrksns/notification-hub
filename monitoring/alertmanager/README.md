# Alertmanager 운영 설정

이 디렉터리는 Prometheus 경보를 Webhook과 SMTP 이메일로 전달하는 Alertmanager 설정을 포함합니다.

## 환경변수

`.env.example`의 이름을 기준으로 로컬 환경이나 배포 Secret에 다음 값을 주입합니다.

| 변수 | 용도 |
| --- | --- |
| `ALERTMANAGER_WEBHOOK_URL` | 경보를 받을 Webhook endpoint |
| `SMTP_SMARTHOST` | SMTP 서버와 포트, 예: `smtp.example.invalid:587` |
| `SMTP_FROM` | SMTP 발신 주소 |
| `SMTP_AUTH_USERNAME` | SMTP 인증 사용자. 일반적으로 비밀이 아닌 관례적인 SMTP 값 |
| `SMTP_AUTH_PASSWORD_FILE` | Alertmanager가 읽을 SMTP 인증 비밀번호 파일 경로 |
| `ALERT_EMAIL_TO` | 경보 수신 주소 |

실제 endpoint, 계정, 비밀번호, 수신 주소는 저장소에 커밋하지 않습니다. 로컬 Secret store 또는 배포 환경의 Secret에서 주입합니다.

## 지원하는 값 형식

`entrypoint.sh`는 Alertmanager가 환경변수를 YAML에 확장하기 전에 필수 비밀번호 파일 경로와 일반 scalar 값을 검사합니다. `SMTP_AUTH_USERNAME`은 일반적으로 비밀이 아닌 관례적인 SMTP 값이므로 별도 파일이 필요하지 않습니다.

Webhook URL, SMTP smarthost, 발신 주소, SMTP 사용자, 수신 주소와 비밀번호 파일 경로는 공백 없이 작성하는 일반적인 YAML plain scalar 형식을 사용합니다. 작은따옴표, 큰따옴표, `#`, `$`, 공백과 줄바꿈은 허용하지 않으며, YAML 문법을 깨는 콜론 형식도 거부합니다. 따라서 `smtp.example.invalid:587`, `https://webhook.example.invalid/alerts`, `alertmanager@example.invalid` 같은 값은 지원하지만 표시 이름이 포함된 주소는 지원하지 않습니다.

`SMTP_AUTH_PASSWORD_FILE`은 YAML scalar로 비밀번호를 확장하지 않고 Alertmanager가 읽는 파일 경로만 전달합니다. 비밀번호 파일 내용에는 따옴표를 포함한 문자를 사용할 수 있습니다.

## 시작 전 검증

컨테이너의 entrypoint로 `entrypoint.sh`를 지정하면 Alertmanager 실행 전에 필수 일반 scalar 값과 비밀번호 파일의 읽기 권한을 검사합니다. 검사를 통과한 경우에만 `/bin/alertmanager --config.file=/etc/alertmanager/alertmanager.yml --config.expand-env`를 실행합니다.

```sh
docker run --rm \
  -e ALERTMANAGER_WEBHOOK_URL=https://webhook.example.invalid/alerts \
  -e SMTP_SMARTHOST=smtp.example.invalid:587 \
  -e SMTP_FROM=alertmanager@example.invalid \
  -e SMTP_AUTH_USERNAME=placeholder \
  -e SMTP_AUTH_PASSWORD_FILE=/run/secrets/smtp-password \
  -e ALERT_EMAIL_TO=alerts@example.invalid \
  -v "$PWD/monitoring/alertmanager:/etc/alertmanager:ro" \
  -v "$PWD/monitoring/alertmanager/entrypoint.sh:/entrypoint.sh:ro" \
  -v "$PWD/.secrets/smtp-password:/run/secrets/smtp-password:ro" \
  --entrypoint /entrypoint.sh \
  prom/alertmanager:latest
```

## 라우팅 동작

기본 route는 `alertname`, `service`, `severity`로 경보를 그룹화합니다. 30초 동안 수집된 경보를 묶어 보내고, 그룹 변경은 5분 간격으로 반영하며, 같은 장애의 반복 알림은 4시간으로 제한합니다.

모든 경보는 `webhook`과 `email` receiver로 각각 전달됩니다. 두 receiver는 독립된 설정이며 복구 시 `send_resolved: true`로 복구 알림도 전송합니다.

## 설정 검증

Alertmanager 이미지에 포함된 도구를 사용해 환경변수를 주입한 상태로 검증합니다.

```sh
docker run --rm \
  -e ALERTMANAGER_WEBHOOK_URL=http://webhook.example.invalid/alerts \
  -e SMTP_SMARTHOST=smtp.example.invalid:587 \
  -e SMTP_FROM=alertmanager@example.invalid \
  -e SMTP_AUTH_USERNAME=placeholder \
  -e SMTP_AUTH_PASSWORD_FILE=/run/secrets/smtp-password \
  -e ALERT_EMAIL_TO=alerts@example.invalid \
  -v "$PWD/monitoring/alertmanager:/etc/alertmanager:ro" \
  -v "$PWD/.secrets/smtp-password:/run/secrets/smtp-password:ro" \
  prom/alertmanager:latest \
  amtool check-config --config.expand-env /etc/alertmanager/alertmanager.yml
```

위 검증 전에 로컬 테스트 디렉터리를 만들고 따옴표가 포함된 테스트 비밀번호 파일을 기록합니다. 예를 들어 `mkdir -p .secrets && printf '%s' 'unsafe'"'"'password" > .secrets/smtp-password`를 사용할 수 있습니다. 실제 비밀번호는 로컬 Secret store 또는 배포 환경의 Secret에서 주입하고 저장소에 기록하지 않습니다.

실제 전송을 확인하려면 유효한 Webhook endpoint와 SMTP Secret을 주입한 뒤 Alertmanager API 또는 Prometheus 경보를 사용합니다. 이 저장소의 Task 3에서 Docker Compose 서비스 연결을 별도로 추가합니다.
