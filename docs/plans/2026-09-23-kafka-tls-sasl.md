# Kafka TLS/SASL 적용 계획

## 목적

현재 Kafka Producer/Consumer와 Kubernetes·AWS MSK가 plaintext로 연결되어 있어 서비스 간 이벤트가 네트워크에서 보호되지 않는다. 로컬 개발 호환성을 유지하면서 production 연결에는 TLS와 SASL 인증을 적용한다.

## 환경별 계약

- 로컬 Compose와 Testcontainers는 기존 `PLAINTEXT` 기본값을 유지한다.
- Kubernetes Kafka는 `SASL_SSL`과 `PLAIN` 인증을 사용하고, PEM 인증서와 SASL JAAS 설정은 Secret에서 주입한다.
- AWS MSK는 broker 간 TLS와 SCRAM-SHA-512 인증을 활성화한다. 애플리케이션은 `SASL_SSL`, `SCRAM-SHA-512`, MSK SCRAM Secret 값을 환경 변수로 주입한다.
- 서비스의 수동 Kafka factory는 Spring 설정의 security protocol, SASL mechanism, JAAS, truststore 값을 Producer와 Consumer 양쪽에 적용한다.

## 범위

- notification-service, delivery-service, analytics-service의 Kafka client 설정.
- Kubernetes Kafka Deployment, ConfigMap, Secret 예시.
- AWS MSK Terraform 모듈과 output.
- 보안 속성 적용 회귀 테스트와 운영 문서.

## 제외 범위

- 로컬 Compose Kafka를 TLS/SASL 전용으로 전환.
- 실제 운영 인증서 발급과 AWS Secrets Manager secret 생성.
- public ingress TLS와 실제 Kubernetes CNI 검증.

## 완료 기준

- 세 서비스의 Producer/Consumer factory가 `SASL_SSL` 등 보안 속성을 전달한다.
- Kubernetes manifest가 `SASL_SSL` listener와 PEM Secret 계약을 정의한다.
- AWS MSK Terraform이 TLS broker encryption과 SCRAM authentication을 활성화한다.
- 로컬 전체 테스트와 정적 release gate가 통과한다.

## 운영 적용 전 확인

운영자는 `kafka-tls` 인증서의 SAN에 `kafka-service`와 실제 MSK broker hostname을 포함하고, AWS MSK SCRAM Secret을 MSK 허용 형식과 KMS 정책으로 등록해야 한다. 인증서 또는 Secret이 없으면 production Kafka client가 연결되지 않는 fail-closed 상태가 된다.
