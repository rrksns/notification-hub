# Kubernetes Kafka TLS 기동 계획

## 목표

실제 Kubernetes live release gate 실행 중 발견된 Kafka TLS/SASL 기동 실패를 이미지 계약에 맞게 수정한다.

## 원인

`apache/kafka:3.7.0`의 container entrypoint는 `SASL_SSL` listener를 사용할 때 PEM 내용만 환경변수로 받지 않는다. keystore/truststore 파일명, credential 파일, JAAS 설정 파일과 `KAFKA_OPTS`가 필요하다.

## 변경

- Kafka Deployment가 Secret을 `/etc/kafka/secrets`에 읽기 전용으로 마운트한다.
- broker는 이미지 entrypoint가 지원하는 PKCS12 keystore/truststore 파일과 credential 파일을 사용하고, 애플리케이션 client용 PEM 인증서 값은 별도로 유지한다.
- PKCS12 binary와 broker 전용 JAAS 파일은 공용 애플리케이션 Secret과 분리된 `notification-hub-kafka-secret`에 둔다. 애플리케이션 Pod의 `envFrom`에 binary Secret을 주입하면 NUL 바이트 환경변수로 기동이 실패한다.
- JAAS 설정 파일 경로를 이미지가 요구하는 `KAFKA_OPTS`로 명시한다.
- 여섯 JVM의 cold start를 고려해 liveness 300초, readiness 240초의 startup 여유를 둔다.
- Secret example에 credential 파일용 키를 추가한다.
- disposable Kubernetes Secret에는 테스트용 인증서와 빈 PEM credential 파일을 주입해 실제 Pod 기동을 검증한다.

## 실행 결과

- broker 전용 binary Secret 분리와 명시적 truststore 설정 후 Kafka Pod가 Ready가 됐다.
- 서비스 JAR를 `mvn clean package`로 재생성하고 이미지 태그를 명시적으로 `${service}:latest`로 생성한 뒤 six-service rollout을 재검증했다.
- Flyway baseline과 Kafka client PEM truststore 설정을 적용한 뒤 모든 서비스가 Ready가 됐다.
- `scripts/release/live-release-gate.sh`가 OrbStack Kubernetes CNI에서 성공했다. 외부 운영 클러스터, rollback, backup restore는 이 실행의 범위가 아니다.

## 완료 조건

- Kafka Pod가 `Running` 상태가 되고 SASL_SSL listener를 기동한다.
- 기존 서비스 Deployment와 Ingress를 배포할 수 있다.
- live gate가 Kafka 기동 실패가 아닌 다음 검증 단계까지 진행한다.
