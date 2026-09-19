# Outbox 관측성 지표 구현 계획

상태: 구현 완료.

1. 완료. 설계 문서와 체크리스트를 작성했다.
2. 완료. notification과 delivery result outbox port에 pending count 계약을 추가했다.
3. 완료. 두 dispatcher에 backlog gauge와 publish failure counter를 연결했다.
4. 완료. Prometheus rule, Grafana dashboard, README를 갱신했다.
5. 완료. focused 테스트, 설정 검증, 전체 Maven 및 Docker E2E를 실행했다.
6. 완료. 구현 결과를 상태 문서에 기록하고 push 및 merge한다.

완료 기준은 두 outbox의 실제 pending 건수와 발행 실패가 서비스 메트릭으로 노출되고, 운영 경보와 대시보드에서 확인 가능하며, 전체 검증이 통과하는 것이다.
