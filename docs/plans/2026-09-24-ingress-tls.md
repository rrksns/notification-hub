# 공개 Kubernetes Ingress TLS 계획

## 목표

공개 API Gateway Ingress가 평문 HTTP를 제공하지 않고 `notification-hub.local`에 대한 TLS 인증서를 반드시 사용하도록 배포 계약을 고정한다.

## 범위

- Ingress에 TLS Secret과 HTTP→HTTPS 리다이렉트 정책을 선언한다.
- 애플리케이션 Secret과 Ingress TLS Secret을 분리한다.
- 저장소 테스트와 release gate가 TLS 계약 누락을 실패 처리한다.
- 운영 문서에 인증서 Secret 생성과 외부 Ingress smoke 절차를 기록한다.

## 제외 범위

- 인증서 발급자 또는 cert-manager 도입.
- 실제 Kubernetes CNI와 외부 LoadBalancer에서의 인증서 발급·갱신 검증.
- iOS 작업.

## 완료 조건

1. `k8s/api-gateway/ingress.yaml`에 `notification-hub-ingress-tls`와 `notification-hub.local`이 선언된다.
2. NGINX Ingress의 SSL redirect와 force SSL redirect가 모두 활성화된다.
3. 테스트와 `scripts/release/release-gate.sh --skip-maven`이 계약 위반을 감지한다.
4. README와 release runbook에 실제 Secret 생성 및 HTTPS smoke 명령이 있다.
