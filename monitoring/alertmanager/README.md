# Alertmanager 운영 설정

이 디렉터리는 Prometheus 경보를 Webhook과 SMTP 이메일로 전달하는 Alertmanager 설정을 포함합니다.

## 환경변수

`.env.example`의 이름을 기준으로 로컬 환경이나 배포 Secret에 다음 값을 주입합니다.

| 변수 | 용도 |
| --- | --- |
| `ALERTMANAGER_WEBHOOK_URL` | 경보를 받을 Webhook endpoint |
| `SMTP_SMARTHOST` | SMTP 서버와 포트, 예: `smtp.example.invalid:587` |
| `SMTP_FROM` | SMTP 발신 주소 |
| `SMTP_AUTH_USERNAME` | SMTP 인증 사용자 |
| `SMTP_AUTH_PASSWORD` | SMTP 인증 비밀번호 |
| `ALERT_EMAIL_TO` | 경보 수신 주소 |

실제 endpoint, 계정, 비밀번호, 수신 주소는 저장소에 커밋하지 않습니다. 로컬 Secret store 또는 배포 환경의 Secret에서 주입합니다.

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
  -e SMTP_AUTH_PASSWORD=placeholder \
  -e ALERT_EMAIL_TO=alerts@example.invalid \
  -v "$PWD/monitoring/alertmanager:/etc/alertmanager:ro" \
  prom/alertmanager:latest \
  amtool check-config --config.expand-env /etc/alertmanager/alertmanager.yml
```

실제 전송을 확인하려면 유효한 Webhook endpoint와 SMTP Secret을 주입한 뒤 Alertmanager API 또는 Prometheus 경보를 사용합니다. 이 저장소의 Task 3에서 Docker Compose 서비스 연결을 별도로 추가합니다.
