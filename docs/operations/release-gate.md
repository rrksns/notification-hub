# 상용 배포 출시 게이트

## 목적

main 커밋을 상용 Kubernetes 클러스터에 배포하기 전에 이미지, 애플리케이션 상태, 네트워크 경계, 복구 가능성을 확인한다. 이 문서는 자동화된 사전검증과 운영 환경에서만 가능한 검증을 분리한다.

## 판정 규칙

다음 조건을 모두 충족해야 출시를 승인한다.

- 대상 이미지가 `GITHUB_SHA` immutable tag로 존재하고 모든 서비스가 같은 tag를 사용한다.
- 모든 Deployment rollout과 actuator health check가 성공한다.
- API Gateway 경유 요청은 성공하고 내부 서비스 Pod 직접 접근은 차단된다.
- 공개 Ingress는 유효한 `notification-hub-ingress-tls` 인증서를 사용하고 HTTP 요청을 HTTPS로 리다이렉트한다.
- rollback을 수행할 때 이전 ReplicaSet이 준비 상태로 복귀한다.
- 최근 백업이 애플리케이션 호스트 외부에 저장되어 있고, 별도 환경 복원 리허설의 RPO/RTO가 목표 이내다.

한 항목이라도 증적이 없으면 배포를 승인하지 않고 `manual_test.md`에 실패 원인과 재검증 일정을 기록한다.

## 1. 사전검증

PR 또는 release 후보 커밋에서 실행한다.

반복 가능한 저장소 수준 검증은 다음 명령으로 실행한다. CI에서는 Maven을 이미 실행했으므로 `--skip-maven`을 사용한다.

```bash
export JWT_SECRET="<base64-encoded-test-secret>"
bash scripts/release/release-gate.sh
```

정적 게이트는 Maven, backup/restore 스크립트, Compose, Kubernetes YAML, Prometheus rule을 검사한다. 이 명령의 성공만으로 실제 클러스터 NetworkPolicy, rollout, smoke test, rollback, 별도 환경 restore가 완료된 것으로 간주하지 않는다.

```bash
mvn clean verify -DskipTests=false
bash -n scripts/backup/backup.sh scripts/backup/restore.sh
scripts/backup/backup.sh --dry-run
```

Kubernetes 매니페스트와 NetworkPolicy를 검증한다.

```bash
kubectl apply --dry-run=client -f k8s/namespace.yaml
kubectl apply --dry-run=client -f k8s/networkpolicy/
kubectl apply --dry-run=client -f k8s/infra/ -f k8s/api-gateway/ \
  -f k8s/user-service/ -f k8s/notification-service/ \
  -f k8s/delivery-service/ -f k8s/analytics-service/
```

Docker가 없는 환경에서는 위 명령의 실패를 성공으로 간주하지 않는다. 명령을 실행할 수 없는 이유를 기록하고, 실제 클러스터 검증을 출시 전 필수 단계로 남긴다.

## 2. 이미지와 배포

CI가 게시한 `${GITHUB_SHA}`를 배포한다. `latest`와 로컬 `imagePullPolicy: Never`는 상용 배포에 사용하지 않는다.

```bash
export IMAGE_TAG="<github-sha>"
export REGISTRY="ghcr.io/rrksns/notification-hub"

for svc in discovery-service api-gateway user-service notification-service delivery-service analytics-service; do
  kubectl set image deployment/"$svc" \
    "$svc"="$REGISTRY/$svc:$IMAGE_TAG" \
    -n notification-hub
  kubectl rollout status deployment/"$svc" \
    -n notification-hub --timeout=180s
done
```

배포 전후 증적을 저장한다.

```bash
kubectl get deploy,pods -n notification-hub -o wide
kubectl rollout history deployment/notification-service -n notification-hub
kubectl get events -n notification-hub --sort-by=.lastTimestamp | tail -50
```

## 3. Smoke test

Gateway와 health endpoint가 준비된 뒤 실행한다. 보호 API는 테스트 전 유효한 JWT를 준비한다.

```bash
kubectl port-forward -n notification-hub svc/api-gateway 18080:8080
curl -fsS http://127.0.0.1:18080/actuator/health
curl -fsS -X POST http://127.0.0.1:18080/api/users/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"release-gate@example.com","password":"change-me-now"}'
```

공개 Ingress 경계도 별도로 확인한다. `${INGRESS_ADDRESS}`는 Ingress Controller의 외부 IP 또는 DNS가 해석되는 주소이며, 테스트 인증서 사용 시 `--cacert` 또는 명시적인 테스트용 `-k` 사용 사유를 기록한다.

```bash
export INGRESS_ADDRESS="<ingress-address>"
curl --fail --resolve notification-hub.local:443:"${INGRESS_ADDRESS}" \
  https://notification-hub.local/actuator/health
curl --fail --silent --show-error --head \
  --resolve notification-hub.local:80:"${INGRESS_ADDRESS}" \
  http://notification-hub.local/actuator/health | grep -E '^HTTP/.* (301|308)'
```

첫 번째 요청은 인증서 검증과 HTTPS health 응답을, 두 번째 요청은 HTTP 요청의 HTTPS 리다이렉트를 증명한다. 인증서 Secret 누락, 인증서 호스트 불일치, 리다이렉트 미동작은 출시 실패로 기록한다.

테스트 tenant로 로그인한 뒤 JWT를 사용해 알림 생성과 delivery 조회를 검증한다. 응답 상태, notification ID, delivery 상태, analytics 집계 결과를 기록한다. 실제 provider가 연결되지 않은 환경에서는 logging provider 검증으로 표시하고 상용 provider 성공으로 기록하지 않는다.

## 4. NetworkPolicy 허용·차단 검증

NetworkPolicy를 적용한 뒤 임시 debug Pod를 사용한다.

```bash
kubectl apply -f k8s/networkpolicy/
kubectl run release-gate-debug -n notification-hub --rm -i --restart=Never \
  --image=curlimages/curl:8.10.1 -- \
  curl --fail --connect-timeout 5 http://api-gateway:8080/actuator/health
```

다음 두 결과를 모두 확인한다.

- `app=api-gateway` Pod에서 내부 서비스 API 포트 접근이 성공한다.
- 임의 debug Pod에서 user, notification, delivery, analytics 서비스 API 포트 접근이 timeout 또는 거부된다.
- 모니터링 namespace의 승인된 Prometheus Pod에서 각 actuator 포트 접근이 성공한다.

클러스터 CNI가 실제로 정책을 집행했는지 확인할 수 없는 경우, manifest 적용 성공만으로 통과 처리하지 않는다.

## 5. Rollback

새 ReplicaSet이 health check나 smoke test를 통과하지 못하면 해당 release를 중단하고 이전 revision으로 되돌린다.

```bash
for svc in discovery-service api-gateway user-service notification-service delivery-service analytics-service; do
  kubectl rollout undo deployment/"$svc" -n notification-hub
  kubectl rollout status deployment/"$svc" \
    -n notification-hub --timeout=180s
done
```

rollback 후 Gateway health, Kafka consumer 정상 처리, DB migration 호환성, DLQ 증가 여부를 다시 확인한다. rollback이 완료되기 전에는 새 이미지 재배포를 승인하지 않는다.

## 6. Backup·restore 증적

백업은 하루 한 번 이상 실행하고 애플리케이션 호스트 외부 저장소에 복제한다.

```bash
scripts/backup/backup.sh --output /secure-backups/notification-hub
scripts/backup/restore.sh \
  --input /secure-backups/notification-hub/<timestamp>
```

월 1회 별도 복원 환경에서 `--confirm` 복원을 실행한다. 다음 값을 기록한다.

| 항목 | 목표 | 실제 기록 |
|---|---:|---|
| 백업 생성 시각 | 24시간 이내 | `manual_test.md`에 기록 |
| 복원 시작·종료 시각 | 60분 이내 | `manual_test.md`에 기록 |
| MySQL·MongoDB 데이터 검증 | 성공 | row/document 검증 결과 |
| Redis·Kafka 복원 검증 | 성공 | key/topic/partition 결과 |
| 애플리케이션 smoke test | 성공 | health·API·DLQ 결과 |

외부 저장소 복제, 별도 복원 환경, 실제 CNI 검증이 준비되지 않은 상태는 구현 완료가 아니라 운영 검증 대기 상태다.

## 증적 보관

출시마다 다음 파일과 명령 결과를 release ticket 또는 보안 저장소에 보관한다.

- 배포 커밋 SHA와 여섯 서비스 이미지 tag.
- rollout history, pod 상태, smoke test 결과.
- NetworkPolicy 허용·차단 결과.
- rollback 수행 여부와 최종 revision.
- backup manifest와 restore RPO/RTO 기록.
