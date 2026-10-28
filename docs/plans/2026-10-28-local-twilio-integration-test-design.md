# Local Twilio Integration Test Design

## Goal

실제 Twilio 계정, 전화번호, 비용 없이 delivery-service가 Spring 설정을 통해 `TwilioSmsSender`를 선택하고 loopback HTTP로 Twilio Messages API 계약을 호출하는지 검증한다.

## Decision

새 테스트 라이브러리를 추가하지 않는다. Java 17의 `com.sun.net.httpserver.HttpServer`를 `127.0.0.1`의 임의 포트에 실행해 가짜 Twilio 서버로 사용한다.

WireMock은 풍부한 매처를 제공하지만 현재 프로젝트에 없고, Docker 컨테이너는 이 테스트의 범위보다 무겁다. JDK 서버는 실제 TCP와 HTTP를 거치면서도 외부 네트워크·자격 증명·비용을 발생시키지 않는다.

## Scope

새 통합 테스트는 아래 계약을 검증한다.

1. `sms.provider=twilio`일 때 Spring이 `TwilioSmsSender`를 선택한다.
2. `twilio.*` 테스트 속성이 `TwilioProperties`에 바인딩된다.
3. `twilio.api-url`이 loopback 가짜 서버 주소로 적용된다.
4. 발송 요청이 `POST /2010-04-01/Accounts/ACtest/Messages.json`으로 도달한다.
5. 요청은 Basic Auth와 `application/x-www-form-urlencoded` 형식을 사용한다.
6. 폼 본문은 `To`, `Body`, `From` 값을 포함한다.
7. 가짜 서버의 `201 Created` 응답이 성공으로 처리된다.

기존 `TwilioSmsSenderTest`는 요청 구성과 4xx·네트워크 오류 단위 테스트로 유지한다. 새 테스트는 이를 대체하지 않는다.

## Non-goals

- 실제 Twilio API 호출과 SMS 수신 확인.
- Kafka 소비, MySQL 저장, Retry·Circuit Breaker를 포함한 전체 E2E.
- Twilio 상태 콜백과 통신사 최종 전달 상태.
- 운영 자격 증명, `.env.local`, 발신 번호 변경.

## Test Shape

테스트 클래스는 `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/`에 둔다.

테스트 메서드 안에서 가짜 서버를 시작하고 종료한다. Spring 테스트 컨텍스트에는 `sms.provider=twilio`과 loopback `twilio.api-url`을 동적으로 제공한다. 가짜 서버 핸들러는 요청 메서드·경로·헤더·폼 본문을 기록하고, 검증 가능한 요청에만 `201 Created`를 반환한다.

성공 조건은 한 번의 `SmsSender.send()`가 정확히 한 번의 loopback 요청을 만들고, 가짜 서버가 기대한 계약을 모두 관찰하는 것이다.

## Safety

테스트는 `127.0.0.1`만 수신하고 `TWILIO_API_URL`을 테스트 실행 중에만 덮어쓴다. 실제 `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, 수신 번호, Messaging Service SID를 읽거나 저장하지 않는다. 테스트 값은 `ACtest`, `test-token`, `+15551234567`, `+821012345678`으로 고정한다.

## Verification

1. 새 테스트가 구현 전 실패하는 것을 확인한다.
2. 새 테스트와 기존 `TwilioSmsSenderTest`를 함께 실행한다.
3. `mvn test -pl delivery-service`를 실행한다.
4. 최종적으로 루트 `mvn test`를 실행한다.
