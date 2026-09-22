# API 키 API 공개 경로 중단 계획

## 목적

API 키는 user-service에서 생성·저장되지만 현재 게이트웨이 또는 하위 서비스의 요청 인증에는 연결되어 있지 않다. 미완성 인증 API를 상용 외부 경로로 노출하지 않도록 이번 범위에서 명시적으로 deprecated 처리한다.

## 범위

- api-gateway의 `/api/keys/**` 공개 라우트를 제거한다.
- user-service의 기존 생성 코드는 보존하되 향후 API 키 인증 계약이 확정되기 전까지 외부 클라이언트 사용을 금지한다.
- 게이트웨이 라우트 회귀 테스트와 API 문서를 현재 공개 surface에 맞춘다.

## 제외 범위

- API 키 검증용 user-service 내부 API 추가.
- API 키 원문 해시 저장 및 키 회전 정책.
- Kafka TLS/SASL 및 public ingress TLS.

## 완료 기준

- 게이트웨이 설정에 `/api/keys/**` 라우트가 없다.
- 라우트 테스트가 API 키 경로의 미노출을 검증한다.
- README와 서비스 흐름 문서가 API 키 생성 API를 deprecated로 표시한다.
- 전체 Maven 테스트와 정적 release gate가 통과한다.

## 운영 후속

API 키를 다시 공개하려면 검증 endpoint의 내부 인증, timeout·fail-closed 정책, revoked/expired 처리, 원문 비저장과 회전 절차를 별도 설계하고 실제 다중 서비스 환경에서 검증해야 한다.
