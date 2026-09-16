# Delivery 결과 Outbox 구현 계획

상태: 구현 완료.

## 구현 단계

1. 완료. Flyway migration에 delivery log 유니크 제약과 결과 outbox 스키마를 추가했다.
2. 완료. 결과 outbox 도메인 모델, port, JPA entity/repository/adapter를 추가했다.
3. 완료. `ProcessDeliveryService`가 결과 publisher 대신 outbox를 저장하도록 변경했다.
4. 완료. Kafka publisher와 scheduled dispatcher를 연결하고 실패 시 pending을 유지한다.
5. 완료. 발송 서비스 단위 테스트와 Docker E2E를 갱신했다.
6. 완료. 전체 Maven 테스트와 E2E를 실행하고 운영 문서를 갱신했다.

## 완료 기준

- 발송 결과 이벤트가 Kafka 장애 후에도 outbox에 남는다.
- 같은 notification의 delivery log가 중복 생성되지 않는다.
- 전체 Maven 테스트와 Docker E2E가 통과한다.
- 우선순위 문서, checklist, context notes에 구현 상태가 반영된다.
