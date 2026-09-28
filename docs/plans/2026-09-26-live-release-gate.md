# Kubernetes live release gate 계획

## 목표

실제 Kubernetes CNI에서 상용 배포에 필요한 rollout, Ingress TLS, HTTPS 경계, NetworkPolicy 허용·차단 결과를 반복 가능한 명령으로 검증한다.

## 범위

- `scripts/release/live-release-gate.sh`를 추가한다.
- 여섯 서비스 Deployment rollout 완료를 확인한다.
- `api-gateway-ingress`가 `notification-hub-ingress-tls` Secret을 사용하고 Secret 타입이 TLS인지 확인한다.
- 외부 주소를 통한 HTTPS health와 HTTP to HTTPS redirect를 확인한다.
- 기본 80/443 포트와 테스트 클러스터의 NodePort를 환경변수로 선택할 수 있다.
- `app=api-gateway` debug Pod는 내부 서비스에 접근할 수 있고, 라벨이 없는 debug Pod는 접근할 수 없는지 확인한다.
- 실패 시 non-zero로 종료하고 임시 Pod를 정리한다.
- 운영 runbook, checklist, context notes에 실행 조건과 현재 환경의 검증 결과를 기록한다.

## 제외 범위

- rollback 자동 실행.
- 별도 환경 backup restore 실행.
- 실제 클러스터가 없는 로컬 환경에서 성공으로 간주하는 우회 처리.
- iOS 작업.

## 완료 조건

- 스크립트 문법 검사와 정적 release gate가 성공한다.
- 필수 `INGRESS_ADDRESS`가 없으면 fail-closed로 종료한다.
- Kubernetes API가 연결되지 않는 현재 환경에서는 실행 불가 사유가 출력되고 성공으로 처리되지 않는다.
- 실제 클러스터 실행 결과는 별도 운영 증적으로 남길 수 있다.

## 2026-09-28 실행 중 발견한 후속 수정

OrbStack Kubernetes에 disposable namespace를 배포하는 과정에서 `apache/kafka:3.7.0`의 TLS/SASL entrypoint가 요구하는 파일 기반 credential과 JAAS 설정이 매니페스트에 없음을 확인했다. 이 계약을 `2026-09-28-k8s-kafka-tls-live-gate.md` 계획으로 분리해 수정한다.
