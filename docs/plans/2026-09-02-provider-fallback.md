# Provider fallback 구현 계획

1. fail-closed fallback 정책 포트를 정의한다.
2. delivery-service의 Circuit Breaker fallback에 정책을 연결한다.
3. fallback 호출과 FAILED 결과 보존을 테스트한다.
4. 전체 테스트 후 운영 문서와 우선순위 상태를 갱신한다.
