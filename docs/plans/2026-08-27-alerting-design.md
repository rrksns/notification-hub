# 알림 및 경보 체계 설계

## 목표

Prometheus가 수집한 장애 신호를 Alertmanager를 통해 범용 Webhook과 SMTP 이메일로 동시에 전달한다. 운영자는 서비스 장애, 5xx 증가, DLQ 적체, 외부 Provider 실패를 대시보드에 접속하지 않고도 인지할 수 있어야 한다.

## 범위

- Prometheus alerting rule 파일 추가.
- Alertmanager 설정 파일 추가.
- Docker Compose에 Alertmanager 서비스와 Prometheus rule 연결 추가.
- Webhook과 이메일 receiver 동시 라우팅.
- SMTP 접속 정보와 Webhook URL의 환경변수 주입.
- 알림 발생, 반복 억제, 복구 알림에 대한 운영 문서와 설정 예시 추가.

Kubernetes용 Alertmanager 배포 리소스와 실제 Webhook 서버 구현은 이번 범위에 포함하지 않는다. Kubernetes 환경에서는 동일한 규칙과 Alertmanager 설정을 ConfigMap 또는 Secret으로 주입할 수 있도록 파일 구조만 재사용 가능하게 유지한다.

## 아키텍처

Prometheus는 15초 주기로 각 서비스의 `/actuator/prometheus`를 수집하고, rule 파일을 평가한다. 조건이 `for` 기간 동안 유지되면 Prometheus가 Alertmanager로 알림을 전달한다. Alertmanager는 기본 receiver 하나에 Webhook과 이메일 receiver를 함께 연결해 모든 알림을 두 채널로 전송한다.

알림 그룹은 `alertname`, `service`, `severity` 기준으로 묶는다. 동일 장애는 `group_wait`와 `group_interval`로 묶어 전송하고, 장기 장애 반복 알림은 `repeat_interval`로 제한한다. 상태가 정상으로 돌아오면 `send_resolved`를 통해 복구 알림을 전송한다.

## 알림 규칙

현재 애플리케이션과 Spring Boot가 노출하는 메트릭을 기준으로 다음 규칙을 제공한다.

| 알림 | 조건 | 심각도 |
|---|---|---|
| ServiceDown | `up == 0`이 2분 지속 | critical |
| Http5xxRateHigh | 최근 5분 HTTP 5xx 비율이 5% 초과이고 요청 수가 존재 | warning |
| DlqMessagesIncreasing | DLQ consumer group의 lag 또는 DLQ 메시지 증가 신호가 임계치를 초과 | critical |
| ProviderFailureRateHigh | Provider 관련 실패 카운터 비율이 10% 초과이고 발송 시도가 존재 | critical |

메트릭이 현재 노출되지 않는 DLQ lag와 Provider 실패 카운터는 규칙을 추가하기 전에 실제 Prometheus 시계열을 확인한다. 시계열이 없으면 존재하지 않는 메트릭을 가정해 무의미한 규칙을 배포하지 않고, 해당 신호를 노출하는 최소 애플리케이션 계측을 별도 작업으로 분리한다.

## 설정과 보안

- `monitoring/alertmanager/alertmanager.yml`은 receiver 구조와 라우팅만 추적한다.
- 실제 SMTP 비밀번호는 파일 기반 Secret으로 주입하고, Webhook URL과 수신 이메일 등 설정값은 `${...}` 환경변수로 주입한다.
- `smtp_auth_password_file`을 사용해 비밀번호가 YAML 문법에 삽입되지 않도록 한다.
- Docker Compose는 Alertmanager를 `9093` 포트로 노출하고 Prometheus의 `alerting` 및 `rule_files` 설정을 사용한다.
- 예시 환경변수는 `.env.example` 또는 README에 문서화하며 실제 Secret은 커밋하지 않는다.

## 오류 처리와 운영 동작

Alertmanager가 수신자 전송에 실패해도 Prometheus의 알림 평가 자체는 유지한다. Webhook과 이메일은 독립된 receiver로 설정해 한 채널의 장애가 다른 채널 전달을 막지 않게 한다. SMTP 또는 Webhook 설정이 없는 개발 환경에서는 해당 receiver를 비활성화할 수 있는 Compose 환경변수 구성을 문서화한다.

## 검증

- Prometheus 설정과 rule 파일의 문법 검증.
- Alertmanager 설정의 문법 검증.
- Docker Compose 구성 렌더링 검증.
- 설정된 컨테이너 기동 후 Prometheus와 Alertmanager의 readiness 확인.
- Alertmanager API에서 로드된 route와 receiver 확인.
- 문서의 환경변수 이름과 실제 설정 키 일치 확인.
