# 핵심 E2E 통합 테스트 설계

## 목표

상용화 P0 4번 `핵심 E2E 통합 테스트 추가`를 진행한다. 단위 테스트만으로는 확인하기 어려운 MySQL, Redis, Kafka, MongoDB 연동 경로를 Testcontainers 기반 자동 테스트로 검증한다.

## 범위

첫 번째 테스트는 `notification-service`의 접수 경로를 검증한다. 실제 MySQL, Redis, Kafka를 띄운 뒤 `CreateNotificationUseCase`를 호출하고, 알림 저장, Redis idempotency 키 저장, Kafka `notifications` 토픽 발행을 확인한다.

두 번째 테스트는 `delivery-service`와 `analytics-service`의 이벤트 파이프라인을 검증한다. 실제 Kafka, MySQL, MongoDB, Redis를 띄운 뒤 `NotificationEvent`를 발행하고, delivery log 저장, `delivery-results` 발행, analytics delivery event 저장, Redis 실시간 카운터 증가를 확인한다.

## 제외 범위

API Gateway와 전체 HTTP 라우팅은 이번 작업에서 제외한다. gateway까지 포함하면 Eureka, 포트, 보안 토큰, 다중 애플리케이션 프로세스 관리가 한 번에 얽혀 테스트가 느리고 불안정해진다. 이번 목표는 저장소와 메시지 브로커를 포함한 핵심 비즈니스 파이프라인을 자동 검증하는 것이다.

실제 SendGrid, Twilio, FCM 호출도 제외한다. delivery-service 기본 provider는 `logging`으로 유지해 외부 계정이나 네트워크 없이 발송 성공 경로를 검증한다.

## 기술 선택

Testcontainers JUnit Jupiter를 사용한다. Docker가 없는 환경에서는 `@Testcontainers(disabledWithoutDocker = true)`로 테스트 클래스를 skip해 일반 개발 환경과 CI 환경 모두에서 실패가 아닌 명시적 skip이 되게 한다.

Spring Boot 3.2의 `spring-boot-testcontainers`와 `@ServiceConnection`을 우선 사용한다. 여러 서비스가 같은 컨테이너 세트를 공유해야 하는 경우에는 `@DynamicPropertySource`로 명시적인 연결값을 주입한다.

## 테스트 구조

`notification-service` 안에 `NotificationAcceptanceIntegrationTest`를 둔다. 이 테스트는 notification-service 애플리케이션 컨텍스트만 띄우며 MySQL, Redis, Kafka 연결을 실제 컨테이너로 대체한다.

`delivery-service`와 `analytics-service` 파이프라인은 테스트 전용 모듈 또는 한 서비스의 통합 테스트에서 다룬다. 두 애플리케이션 컨텍스트를 동시에 띄우는 방식이 과도하면, delivery-service는 실제 Kafka/MySQL로 검증하고 analytics-service는 실제 MongoDB/Redis/Kafka 소비 컨텍스트를 별도 테스트로 분리한다. 핵심 원칙은 테스트가 실제 인프라 어댑터를 통과해야 한다는 점이다.

## 성공 기준

- `notification-service` 통합 테스트가 Flyway migration 적용 후 Hibernate validate를 통과한다.
- 알림 생성 결과가 `PUBLISHED`이고 MySQL `notifications` 테이블에 저장된다.
- Redis에 `idempotency:notification:{tenantId}:{idempotencyKey}` 키가 저장된다.
- Kafka `notifications` 토픽에서 같은 notification id와 tenant id를 가진 이벤트를 읽을 수 있다.
- 파이프라인 테스트가 delivery log `SUCCESS`, analytics delivery event 저장, Redis success counter 증가를 관찰한다.
- 기존 `mvn test`는 Docker 유무와 관계없이 실패 없이 완료된다.
