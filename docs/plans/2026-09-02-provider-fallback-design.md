# Provider fallback 설계

## 목표

외부 provider 장애가 발생했을 때 재시도 소진 후의 동작을 명시적으로 기록하고, 실제 발송 성공으로 잘못 처리되지 않도록 한다.

## 범위

- 기존 Resilience4j 재시도와 Circuit Breaker를 primary provider 보호 계층으로 유지한다.
- 재시도 소진 시 fallback 정책이 채널, 수신자, 원인을 운영 로그로 기록한다.
- fallback은 현재 대체 provider가 없으므로 성공을 반환하지 않고 원래 예외를 다시 던진다.
- 실제 대체 provider, 지연 발송, 고객 알림은 후속 범위로 남긴다.

## 검증 기준

- fallback 정책이 실패 정보를 전달받는다.
- fallback 이후에도 `ProcessDeliveryService`가 FAILED 결과를 저장하고 failure 이벤트를 발행한다.
- 전체 Maven 테스트가 통과한다.
