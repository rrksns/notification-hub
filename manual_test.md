# Manual Test 기록

**테스트 일자**: 2026-03-19 (Phase 3~4), 2026-03-20 (Phase 5), 2026-03-24 (Phase 6 - k8s/CI/모니터링), 2026-07-11 (SendGrid EMAIL 실제 발송), 2026-07-15 (Twilio SMS 실제 발송 준비), 2026-07-17 (Android FCM 실제 발송 준비), 2026-07-18 (SMS/PUSH 실패 흐름 검증)
**테스트 환경**: 로컬 (MacOS), docker-compose 인프라 기동 상태 / OrbStack Kubernetes

---

## SendGrid EMAIL 실제 발송 검증 (2026-07-11)

### 목적

delivery-service의 EMAIL provider가 SendGrid Mail Send API와 실제로 연동 가능한지 확인했습니다.

### 사전 조건

- SendGrid API Key 생성 완료
- Sender Identity 인증 완료
- `.env.local`에 `EMAIL_PROVIDER=sendgrid`, `SENDGRID_API_KEY`, `SENDGRID_FROM_EMAIL`, `SENDGRID_FROM_NAME`, `SENDGRID_SUBJECT` 설정
- API Key와 개인 이메일 주소는 문서에 기록하지 않음

### 검증 과정

1. Sender Identity 인증 전 직접 Mail Send API 호출
2. SendGrid 응답: `403`, `from address does not match a verified Sender Identity`
3. SendGrid Sender Identity 인증 완료
4. `.env`를 `.env.local`에 다시 반영
5. 직접 Mail Send API 호출 재시도
6. SendGrid 응답: `202 Accepted`
7. 테스트 수신 메일함에서 실제 메일 수신 확인

### 결과

- [x] SendGrid API Key 동작 확인
- [x] Sender Identity 인증 필요 조건 확인
- [x] Sender Identity 인증 후 `202 Accepted` 확인
- [x] 실제 수신 메일 확인

### 직접 호출 명령 형태

```bash
set -a
. ./.env.local
set +a

jq -n \
  --arg to "recipient@example.com" \
  --arg from "$SENDGRID_FROM_EMAIL" \
  --arg name "$SENDGRID_FROM_NAME" \
  --arg subject "$SENDGRID_SUBJECT" \
  --arg content "Notification Hub SendGrid actual delivery test." \
  '{personalizations:[{to:[{email:$to}]}], from:{email:$from,name:$name}, subject:$subject, content:[{type:"text/plain", value:$content}]}' \
| curl -sS -o /tmp/sendgrid-response.json -w "%{http_code}\n" \
  -X POST "${SENDGRID_API_URL:-https://api.sendgrid.com/v3/mail/send}" \
  -H "Authorization: Bearer ${SENDGRID_API_KEY}" \
  -H "Content-Type: application/json" \
  --data @-
```

---

## Twilio SMS 실제 발송 준비 (2026-07-15)

### 목적

delivery-service의 SMS provider가 Twilio Messages API와 연동 가능한 구조인지 확인하기 위한 수동 검증 절차입니다.

### 사전 조건

- Twilio Account SID 확인
- Twilio Auth Token 확인
- Twilio 발신 번호 또는 Messaging Service SID 준비
- 테스트 수신 전화번호 준비
- `.env.local`에 `SMS_PROVIDER=twilio`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` 또는 `TWILIO_MESSAGING_SERVICE_SID` 설정
- API Key와 개인 전화번호는 문서에 기록하지 않음

### 직접 호출 명령 형태

```bash
set -a
. ./.env.local
set +a

curl -sS -o /tmp/twilio-response.json -w "%{http_code}\n" \
  -X POST "${TWILIO_API_URL:-https://api.twilio.com/2010-04-01}/Accounts/${TWILIO_ACCOUNT_SID}/Messages.json" \
  -u "${TWILIO_ACCOUNT_SID}:${TWILIO_AUTH_TOKEN}" \
  --data-urlencode "To=+821012345678" \
  --data-urlencode "Body=Notification Hub Twilio actual delivery test." \
  --data-urlencode "From=${TWILIO_FROM_NUMBER}"
```

Messaging Service를 사용하는 경우 `From` 대신 `MessagingServiceSid`를 사용합니다.

```bash
set -a
. ./.env.local
set +a

curl -sS -o /tmp/twilio-response.json -w "%{http_code}\n" \
  -X POST "${TWILIO_API_URL:-https://api.twilio.com/2010-04-01}/Accounts/${TWILIO_ACCOUNT_SID}/Messages.json" \
  -u "${TWILIO_ACCOUNT_SID}:${TWILIO_AUTH_TOKEN}" \
  --data-urlencode "To=+821012345678" \
  --data-urlencode "Body=Notification Hub Twilio actual delivery test." \
  --data-urlencode "MessagingServiceSid=${TWILIO_MESSAGING_SERVICE_SID}"
```

### 예상 결과

- Twilio API 성공 응답은 `201 Created`
- 실패 시 `/tmp/twilio-response.json`에서 Twilio 오류 메시지 확인
- 실제 수신 전화번호로 SMS 수신 확인

### 현재 상태

- [x] Twilio sender 단위 테스트로 요청 형식, Basic Auth, 오류 처리를 검증
- [ ] 실제 Twilio 계정으로 SMS 발송 검증
- [ ] 테스트 수신 전화번호에서 SMS 수신 확인

---

## Android FCM 실제 발송 준비 (2026-07-17)

### 목적

delivery-service의 PUSH provider가 Android FCM HTTP v1 API와 연동 가능한 구조인지 확인하기 위한 수동 검증 절차입니다.

### 사전 조건

- Firebase project id 확인
- FCM HTTP v1 API 활성화
- Firebase service account JSON 준비
- Android 앱에서 발급된 FCM registration token 준비
- `.env.local`에 `PUSH_PROVIDER=fcm`, `FCM_PROJECT_ID`, `GOOGLE_APPLICATION_CREDENTIALS` 또는 `FCM_CREDENTIALS_JSON`, `FCM_TITLE` 설정
- service account JSON과 registration token은 문서에 기록하지 않음

### 직접 호출 명령 형태

`ACCESS_TOKEN`은 Firebase service account로 발급한 `https://www.googleapis.com/auth/firebase.messaging` scope의 OAuth access token입니다.

```bash
set -a
. ./.env.local
set +a

jq -n \
  --arg token "ANDROID_FCM_REGISTRATION_TOKEN" \
  --arg title "${FCM_TITLE:-Notification Hub}" \
  --arg body "Notification Hub Android FCM actual delivery test." \
  '{message:{token:$token, notification:{title:$title, body:$body}}}' \
| curl -sS -o /tmp/fcm-response.json -w "%{http_code}\n" \
  -X POST "${FCM_API_URL:-https://fcm.googleapis.com/v1}/projects/${FCM_PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  --data @-
```

### 애플리케이션 경유 검증 형태

```bash
set -a
. ./.env.local
set +a

mvn spring-boot:run -pl delivery-service
```

notification-service를 통해 `channel=PUSH`, `recipient=ANDROID_FCM_REGISTRATION_TOKEN`으로 알림을 발행하면 delivery-service가 Android FCM token 대상으로 발송합니다.

### 예상 결과

- FCM API 성공 응답은 `200 OK`
- 실패 시 `/tmp/fcm-response.json`에서 FCM 오류 메시지 확인
- 실제 Android 기기에서 PUSH 알림 수신 확인

### 현재 상태

- [x] FCM sender 단위 테스트로 요청 형식, Bearer token, 오류 처리를 검증
- [x] Google service account 기반 access token provider 구현
- [ ] 실제 Firebase project와 Android registration token으로 PUSH 발송 검증
- [ ] Android 기기에서 PUSH 알림 수신 확인

---

## SMS/PUSH 실패 흐름 검증 (2026-07-18)

### 목적

SMS/PUSH provider 예외가 기존 delivery 실패 흐름으로 연결되는지 확인했습니다.

### 검증 기준

- `ChannelDelivererPort.deliver()`에서 발생한 예외는 채널 종류와 무관하게 `ProcessDeliveryService`에서 처리
- 실패 시 `DeliveryLog`는 `FAILED`로 저장
- 실패 시 `DeliveryResultEvent.failure`가 `delivery-results` 토픽 발행 포트로 전달

### 검증 결과

- [x] `ProcessDeliveryServiceTest.process_deliveryFails_savesFailedAndPublishes`가 채널 공통 provider 실패 흐름을 검증
- [x] SMS provider 예외는 `SmsDeliveryException`으로 기존 실패 흐름에 연결
- [x] PUSH provider 예외는 `PushDeliveryException`으로 기존 실패 흐름에 연결
- [x] `mvn test -pl delivery-service` 결과 39개 테스트 통과

새 SMS/PUSH 전용 실패 테스트는 추가하지 않았습니다. 실패 처리 로직이 채널별 분기가 아니라 `ChannelDelivererPort` 예외를 공통으로 받는 구조라서 기존 애플리케이션 테스트가 같은 동작을 이미 검증하기 때문입니다.

---

## 사전 준비

```bash
# 인프라 기동
cd notification-hub
docker-compose up -d

# 서비스 기동 (각각 별도 터미널)
cd notification-hub && mvn spring-boot:run -pl notification-service
cd notification-hub && mvn spring-boot:run -pl delivery-service
```

---

## Phase 3: notification-service

### 테스트 1 — POST /api/notifications → 201 + Kafka 발행

**요청**
```bash
curl -X POST http://localhost:8082/api/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-001" \
  -d '{"channel":"EMAIL","recipient":"user@example.com","content":"Hello, notification test","idempotencyKey":"key-001"}'
```

**결과** ✅
```json
{
  "success": true,
  "data": {
    "notificationId": "d5076281-31f0-4d74-a1e1-cb403d2e0ff3",
    "status": "PUBLISHED"
  }
}
```

---

### 테스트 2 — 동일 idempotencyKey 재요청 → 409 Conflict

**요청** (테스트 1과 동일한 idempotencyKey)
```bash
curl -X POST http://localhost:8082/api/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-001" \
  -d '{"channel":"EMAIL","recipient":"user@example.com","content":"Hello, notification test","idempotencyKey":"key-001"}'
```

**결과** ✅
```json
{
  "success": false,
  "message": "Duplicate notification request"
}
```

**중복 판단 기준**: Redis에 `idempotency:notification:{tenantId}:{idempotencyKey}` 키 존재 여부 (TTL 24시간)

---

### 테스트 3 — GET /api/notifications/{id} → 200 + 알림 상세

**요청**
```bash
curl http://localhost:8082/api/notifications/d5076281-31f0-4d74-a1e1-cb403d2e0ff3 \
  -H "X-Tenant-Id: tenant-001"
```

**결과** ✅ (수정 후)
```json
{
  "success": true,
  "data": {
    "id": "d5076281-31f0-4d74-a1e1-cb403d2e0ff3",
    "tenantId": "tenant-001",
    "channel": "EMAIL",
    "recipient": "user@example.com",
    "content": "Hello, notification test",
    "status": "PUBLISHED"
  }
}
```

**발견된 버그 및 수정**
| 버그 | 원인 | 수정 |
|------|------|------|
| GET 500 에러 | `@PathVariable` 이름 미명시 (`-parameters` 플래그 없음) | `@PathVariable("id")` 명시 |

---

## Phase 4: delivery-service

### 테스트 1 — Kafka 메시지 소비 → DeliveryLog DB 저장 확인

**방법**: notification-service POST 후 delivery-service 로그 및 API 확인

```bash
curl http://localhost:8083/api/deliveries \
  -H "X-Tenant-Id: tenant-001"
```

**delivery-service 로그**
```
Received NotificationEvent: notificationId=..., channel=EMAIL
Delivery processed: deliveryLogId=..., status=SUCCESS
```

**결과** ✅ Kafka 소비 후 DB 저장 확인

---

### 테스트 2 — 발송 결과 → delivery-results 토픽 발행 확인

**방법**: 터미널 2개 사용

터미널 A (구독 대기):
```bash
docker exec -it notification-hub-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic delivery-results --from-beginning
```

터미널 B (알림 발송):
```bash
curl -X POST http://localhost:8082/api/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-001" \
  -d '{"channel":"EMAIL","recipient":"user@example.com","content":"Hello","idempotencyKey":"key-002"}'
```

**결과** ✅ 터미널 A에 DeliveryResultEvent JSON 수신 확인

---

### 테스트 3 — 외부 API 실패 3회 → DLQ 이동 확인

**테스트용 임시 코드 수정**

1. `ChannelDelivererAdapter.java` — `@CircuitBreaker`, `@Retry` 주석 처리, `sendEmail()` 예외 발생
2. `ProcessDeliveryService.java` — catch 블록에 `throw new RuntimeException(e)` 추가

**방법**: 터미널 2개 사용

터미널 A (DLQ 구독):
```bash
docker exec -it notification-hub-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic notifications.dlq --from-beginning
```

터미널 B (알림 발송):
```bash
curl -X POST http://localhost:8082/api/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant-001" \
  -d '{"channel":"EMAIL","recipient":"user@example.com","content":"DLQ Test","idempotencyKey":"key-dlq-001"}'
```

**delivery-service 로그**
```
Received NotificationEvent: notificationId=f0adb59e-...   ← 1차 (notifications)
Published DeliveryResultEvent (FAILED): ...
Received NotificationEvent: notificationId=f0adb59e-...   ← 2차 (notifications-retry-1000, 1초 후)
Published DeliveryResultEvent (FAILED): ...
Received NotificationEvent: notificationId=f0adb59e-...   ← 3차 (notifications-retry-2000, 2초 후)
Published DeliveryResultEvent (FAILED): ...
Record won't be retried. Sending to DLT with name notifications.dlq
Received message in dlt listener: notifications.dlq-0@0
```

**결과** ✅ 3회 재시도 후 notifications.dlq 이동, 터미널 A 수신 확인

**테스트 후 임시 코드 원복 필요**

---

### 테스트 4 — Circuit Breaker OPEN → fallback 동작 확인

**테스트용 임시 코드 수정**

- `ChannelDelivererAdapter.java` — `sendEmail()`에만 예외 발생 (ProcessDeliveryService rethrow 없음)
- `@CircuitBreaker`, `@Retry`는 유지

**설정**: `sliding-window-size: 10`, `failure-rate-threshold: 50%` → 10건 중 5건 실패 시 OPEN

**방법**: 10건 연속 발송
```bash
for i in $(seq 1 10); do curl -s -X POST http://localhost:8082/api/notifications -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-001" -d "{\"channel\":\"EMAIL\",\"recipient\":\"u@test.com\",\"content\":\"CB Test\",\"idempotencyKey\":\"key-cb-00$i\"}" ; sleep 4; done
```

**delivery-service 로그**
```
# 1번째: HALF-OPEN 상태에서 1회 통과 후 다시 OPEN
Delivery processed: status=SUCCESS

# 2번째~10번째: Circuit OPEN 상태
[FALLBACK] Circuit OPEN — channel=EMAIL, recipient=u@test.com, reason=External API unavailable
```

**결과** ✅ Circuit OPEN 후 deliverFallback() 동작 확인

**테스트 후 임시 코드 원복 필요**

---

---

## Phase 5: analytics-service (2026-03-20)

### 사전 준비

```bash
# analytics-service 기동
cd notification-hub && mvn spring-boot:run -pl analytics-service 2>&1 | tee /tmp/analytics.log

# 서비스 기동 확인
curl http://localhost:8084/actuator/health
```

---

### 테스트 1 — Kafka `delivery-results` → MongoDB 저장 확인

**방법**: 알림 발송 후 analytics-service 로그 및 MongoDB 직접 확인

**알림 발송**
```bash
curl -X POST http://localhost:8082/api/notifications -H "Content-Type: application/json" -H "X-Tenant-Id: tenant-001" -d '{"channel":"EMAIL","recipient":"user@example.com","content":"analytics test","idempotencyKey":"analytics-test-001"}'
```

**analytics-service 로그 확인 (기대)**
```
Received delivery result: {deliveryLogId} status=SUCCESS
```

**MongoDB 저장 확인**
```bash
docker exec notification-hub-mongodb mongosh \
  -u nhub -p nhub1234 --authenticationDatabase admin \
  --eval "db.getSiblingDB('analytics').delivery_events.find().sort({occurredAt:-1}).limit(3).pretty()"
```

**결과** ✅
```json
[
  {
    "_id": "471f1102-...",
    "deliveryLogId": "9e0d9e50-...",
    "notificationId": "94b0259c-...",
    "tenantId": "tenant-001",
    "channel": "EMAIL",
    "status": "SUCCESS",
    "occurredAt": "2026-03-20T05:25:33.032Z"
  }
]
```

**트러블슈팅 과정**
- analytics-service 로그에 아무 기록이 없어 Kafka 미수신으로 오인
- `kafka-consumer-groups.sh --describe --group analytics-service` 확인 결과 LAG=0, OFFSET=19 — 실제로는 정상 소비 중
- MongoDB 조회 시 DB 이름을 `analyticsdb`로 잘못 입력하여 빈 결과 → `analytics`로 수정 후 정상 확인
- 소스 코드 문제 없음, 조회 명령 오타가 원인

---

### 테스트 2 — Redis INCR 카운터 증가 확인

```bash
docker exec notification-hub-redis redis-cli GET "realtime:tenant-001:success:total"
```

**결과** ✅ `15` (누적 발송 성공 건수)

**Redis 키 패턴** (실제 구현 기준)
| 키 | 설명 |
|----|------|
| `realtime:{tenantId}:success:total` | 테넌트 전체 성공 카운터 |
| `realtime:{tenantId}:failure:total` | 테넌트 전체 실패 카운터 |
| `realtime:{tenantId}:success:{channel}` | 채널별 성공 카운터 |
| `realtime:{tenantId}:failure:{channel}` | 채널별 실패 카운터 |

---

### 테스트 3 — GET /api/analytics/realtime → 200 + 카운터 응답

```bash
curl -s http://localhost:8084/api/analytics/realtime -H "X-Tenant-Id: tenant-001" | jq .
```

**결과** ✅
```json
{
  "success": true,
  "data": {
    "totalSuccess": 15,
    "totalFailed": 0,
    "totalSent": 15
  }
}
```

---

### 테스트 4 — GET /api/analytics/daily → 200 + DailyStats

```bash
curl -s "http://localhost:8084/api/analytics/daily?date=2026-03-20" -H "X-Tenant-Id: tenant-001" | jq .
```

**결과** ✅ (버그 수정 후)
```json
{
  "success": true,
  "data": {
    "tenantId": "tenant-001",
    "date": "2026-03-20",
    "totalSent": 2,
    "totalSuccess": 2,
    "totalFailed": 0,
    "channelStats": {
      "EMAIL": {
        "successCount": 2,
        "failureCount": 0
      }
    }
  }
}
```

**트러블슈팅 과정**
- 초기 응답: `{"success": false, "message": "Internal server error"}`
- `GlobalExceptionHandler`가 예외를 로깅 없이 삼키고 있어 원인 파악 불가
- `handleException`에 `log.error("Unexpected error", e)` 추가 후 재기동
- 에러 확인: `IllegalArgumentException: Name for argument of type [java.time.LocalDate] not specified`
- 원인: `@RequestParam` 이름 미명시 + 컴파일러 `-parameters` 플래그 없음
- 수정: `@RequestParam("date")` 명시 후 정상 동작

---

---

## Phase 6: k8s 배포 / CI 파이프라인 / 모니터링 (2026-03-24)

**테스트 환경**: OrbStack Kubernetes (v1.33.5), 6GB 메모리

---

### 테스트 1 — GitHub Actions 파이프라인 배지 확인

**방법**
```bash
gh run list --repo rrksns/notification-hub --limit 5
```

**결과** ✅ `completed / success` — CI 배지 정상 표시

---

### 테스트 2 — Terraform local init 검증

**방법**
```bash
terraform -chdir=terraform/local init
```

**결과** ✅ `Terraform has been successfully initialized!`

**비고**: 인프라 컨테이너(MySQL/Redis/MongoDB/Kafka)가 이미 docker-compose로 기동 중이므로 `apply`는 충돌 방지를 위해 생략. init 성공으로 IaC 정의 검증 완료.

---

### 테스트 3 — kubectl get pods → 전체 Running

**사전 작업**

| 작업 | 내용 |
|------|------|
| Dockerfile 베이스 이미지 수정 | `eclipse-temurin:17-jre-alpine` → `eclipse-temurin:17-jre-jammy` (Apple Silicon ARM64 호환) |
| pom.xml 수정 | 부모 pom `pluginManagement`에 `spring-boot:repackage` execution 추가 (fat JAR 생성 누락 수정) |
| k8s deployment 수정 | `imagePullPolicy: Never` 추가, `initialDelaySeconds` 120s로 증가, 메모리 limits 384Mi |
| k8s infra 매니페스트 추가 | `k8s/infra/` — MySQL/Redis/MongoDB/Kafka k8s 배포 파일 신규 생성 |
| MySQL DB 초기화 | k8s MySQL에 `notification_service`, `user_service`, `delivery_service` DB 수동 생성 |
| HPA minReplicas 조정 | 1로 축소 후 HPA 삭제 (메모리 부족으로 인한 OOM Kill 방지) |
| OrbStack 메모리 증가 | 4GB → 6GB |

**방법**
```bash
kubectl get pods -n notification-hub
```

**결과** ✅
```
analytics-service      1/1     Running
api-gateway            1/1     Running
delivery-service       1/1     Running
discovery-service      1/1     Running
kafka                  1/1     Running
mongodb                1/1     Running
mysql                  1/1     Running
notification-service   1/1     Running
redis                  1/1     Running
user-service           1/1     Running
```

**트러블슈팅 과정**
| 문제 | 원인 | 해결 |
|------|------|------|
| 이미지 빌드 실패 | `eclipse-temurin:17-jre-alpine` ARM64 미지원 | `17-jre-jammy`로 교체 |
| JAR 3KB (실행 불가) | 부모 pom에 `spring-boot:repackage` execution 미설정 | pluginManagement에 execution 추가 |
| ErrImageNeverPull | OrbStack k8s containerd가 Docker 이미지 공유 안 됨 | 컨테이너 런타임이 Docker임을 확인, 태그 재설정으로 해결 |
| DB 연결 실패 | k8s MySQL에 서비스별 DB 미생성 | kubectl exec으로 직접 DB 생성 |
| OOM Kill (Exit 137) | 레플리카 2개 × 6서비스 = 메모리 초과 | replicas: 1 축소, OrbStack 메모리 6GB로 증가 |
| API 서버 타임아웃 | 파드 OOM 재시작 폭풍으로 API 과부하 | k8s 재시작 후 안정화 |

---

### 테스트 4 — kubectl get hpa → HPA 동작 확인

**사전 작업**: metrics-server 설치 및 `--kubelet-insecure-tls` 패치

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deployment metrics-server -n kube-system --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
```

**방법**
```bash
kubectl get hpa -n notification-hub
```

**결과** ✅
```
NAME                       REFERENCE                         TARGETS       MINPODS   MAXPODS   REPLICAS
notification-service-hpa   Deployment/notification-service   cpu: .../70%  1         10        1
user-service-hpa           Deployment/user-service           cpu: .../70%  1         10        1
```

```bash
kubectl top nodes
# NAME       CPU(cores)   CPU(%)   MEMORY(bytes)   MEMORY(%)
# orbstack   778m         9%       3760Mi          62%
```

---

### 테스트 5 — Grafana 대시보드 JVM + 비즈니스 메트릭 시각화

**사전 작업**: kubectl port-forward로 k8s 서비스를 로컬 포트에 노출

```bash
kubectl port-forward -n notification-hub svc/discovery-service 8761:8761 &
kubectl port-forward -n notification-hub svc/api-gateway 8080:8080 &
kubectl port-forward -n notification-hub svc/user-service 8081:8081 &
kubectl port-forward -n notification-hub svc/notification-service 8082:8082 &
kubectl port-forward -n notification-hub svc/delivery-service 8083:8083 &
kubectl port-forward -n notification-hub svc/analytics-service 8084:8084 &
docker compose up -d grafana prometheus
```

**접속**: `http://localhost:3000` (admin / admin1234)

**JVM 메트릭 확인**
- Explore → `jvm_memory_used_bytes` → 그래프 정상 출력 ✅

**비즈니스 메트릭 확인**
- Explore → `http_server_requests_seconds_count` → 그래프 정상 출력 ✅

**비고**: Prometheus가 `host.docker.internal:{port}` 방식으로 스크랩하므로 kubectl port-forward 필수. k8s 환경에서 영구 운영 시 Prometheus를 k8s 내부에 배포하는 것이 정석.

---

## 발견된 이슈 및 수정 사항

| # | 서비스 | 이슈 | 수정 내용 |
|---|--------|------|-----------|
| 1 | 공통 | `JwtProperties` 빈 등록 누락으로 기동 실패 | notification/delivery/analytics-service에 `@EnableConfigurationProperties` 추가 |
| 2 | 공통 | `application.yml`에 `jwt` 설정 블록 누락 | 3개 서비스 yml에 jwt 설정 추가 |
| 3 | notification-service | GET `@PathVariable` 이름 미명시로 500 에러 | `@PathVariable("id")` 명시 |
| 4 | delivery-service | GET `@PathVariable` 이름 미명시 | `@PathVariable("id")` 명시 |
| 5 | notification-service | 멱등성 키에 `tenantId` 미포함 — 테넌트 간 키 충돌 가능 | Redis 키를 `{tenantId}:{idempotencyKey}` 구조로 변경 |
| 6 | analytics-service | `@RequestParam` 이름 미명시로 500 에러 (`-parameters` 플래그 없음) | `@RequestParam("date")` 명시 |
| 7 | 공통 | `GlobalExceptionHandler`가 예외 로깅 없이 삼킴 — 디버깅 불가 | `handleException`에 `log.error("Unexpected error", e)` 추가 |
