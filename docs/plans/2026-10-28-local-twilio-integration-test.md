# Local Twilio Integration Test Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 실제 Twilio 호출 없이 delivery-service의 Spring 설정과 loopback HTTP 기반 Twilio Messages API 계약을 검증한다.

**Architecture:** Java 17 `HttpServer`를 테스트 중 `127.0.0.1` 임의 포트에서 실행한다. `ApplicationContextRunner`가 `sms.provider=twilio`과 `twilio.*` 속성으로 `TwilioSmsSender`를 생성하고, 실제 `RestClient`가 가짜 서버로 요청하도록 한다.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Boot Test, JUnit 5, AssertJ, JDK `HttpServer`, Maven.

---

### Task 1: Loopback Twilio 통합 테스트 작성

**Files:**

- Create: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/TwilioSmsSenderIntegrationTest.java`
- Test: `delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/TwilioSmsSenderIntegrationTest.java`

**Step 1: 실패하는 통합 테스트를 작성한다.**

`ApplicationContextRunner`를 `RestClientAutoConfiguration`, `TwilioProperties`, `SmsDeliveryProperties`, `TwilioSmsSender`로 구성한다. 존재하지 않는 `LocalTwilioServer` fixture를 참조해 테스트의 계약을 먼저 고정한다.

```java
contextRunner
        .withPropertyValues(
                "sms.provider=twilio",
                "twilio.account-sid=ACtest",
                "twilio.auth-token=test-token",
                "twilio.from-number=+15551234567",
                "twilio.api-url=" + server.apiUrl())
        .run(context -> {
            assertThat(context).hasSingleBean(TwilioSmsSender.class);
            context.getBean(SmsSender.class).send("+821012345678", "integration test");
        });
```

가짜 서버는 `POST /2010-04-01/Accounts/ACtest/Messages.json`, Basic Auth, form URL-encoded `To`, `Body`, `From` 값을 관찰한다.

**Step 2: 실패를 확인한다.**

Run:

```bash
mvn test -pl delivery-service -Dtest=TwilioSmsSenderIntegrationTest
```

Expected: `LocalTwilioServer` fixture가 아직 없어 컴파일 또는 테스트가 실패한다.

**Step 3: 최소 loopback fixture를 구현한다.**

같은 테스트 클래스의 private static helper로 `HttpServer`를 구현한다. `InetAddress.getLoopbackAddress()`와 포트 `0`을 사용하고, `/2010-04-01/Accounts/ACtest/Messages.json` context만 등록한다. 핸들러는 method, URI path, Authorization, Content-Type, UTF-8 request body를 record에 저장한 뒤 `201`과 빈 body를 응답한다.

```java
server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
server.createContext("/2010-04-01/Accounts/ACtest/Messages.json", exchange -> {
    request.set(new RecordedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
            exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
            exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
    exchange.sendResponseHeaders(HttpStatus.CREATED.value(), -1);
    exchange.close();
});
server.start();
```

테스트 구성은 필요한 Bean만 등록한다. `DeliveryServiceApplication` 전체를 시작하지 않아 Kafka, MySQL, Eureka를 요구하지 않는다.

```java
private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
        .withUserConfiguration(TestConfiguration.class, TwilioSmsSender.class);

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({SmsDeliveryProperties.class, TwilioProperties.class})
static class TestConfiguration {
}
```

**Step 4: 새 테스트와 기존 sender 단위 테스트를 검증한다.**

Run:

```bash
mvn test -pl delivery-service -Dtest=TwilioSmsSenderIntegrationTest,TwilioSmsSenderTest
```

Expected: loopback 서버가 정확히 한 요청을 관찰하고 두 테스트 클래스가 모두 통과한다.

### Task 2: 검증 문서와 작업 기록을 갱신한다.

**Files:**

- Modify: `delivery-service/DELIVERY-SERVICE-FLOW.md`
- Modify: `checklist.md`
- Modify: `context-notes.md`

**Step 1: flow 문서에 로컬 검증 절을 추가한다.**

실제 계정 없이 `mvn test -pl delivery-service -Dtest=TwilioSmsSenderIntegrationTest`를 실행해 loopback HTTP 계약을 검증한다는 점과, 이것이 통신사 최종 전달 증명은 아니라는 점을 기록한다.

**Step 2: 체크리스트와 컨텍스트를 갱신한다.**

`Local Twilio Integration Test` 항목에서 구현과 검증을 완료로 표시하고, loopback-only 보안 경계 및 외부 비발송 결정을 context notes에 남긴다.

**Step 3: delivery-service 검증을 실행한다.**

Run:

```bash
mvn test -pl delivery-service
```

Expected: delivery-service의 기존 및 새 테스트가 모두 통과한다.

**Step 4: 전체 Maven 검증을 실행한다.**

Run:

```bash
mvn test
```

Expected: 전체 모듈이 통과한다. Docker가 없는 환경에서 명시적으로 skip되는 E2E는 별도로 기록한다.

### Task 3: 논리 단위로 커밋한다.

**Files:**

- Modify: Task 1과 Task 2의 파일만 포함한다.

**Step 1: 변경 범위를 검토한다.**

Run:

```bash
git status --short
git diff --check
```

Expected: 자격 증명, `.env.local`, 운영 설정 파일은 변경 목록에 없다.

**Step 2: 커밋한다.**

Run:

```bash
git add delivery-service/src/test/java/com/notificationhub/delivery/infrastructure/sender/TwilioSmsSenderIntegrationTest.java delivery-service/DELIVERY-SERVICE-FLOW.md checklist.md context-notes.md
git commit -m "test: add local Twilio integration coverage"
```

Expected: loopback-only SMS 계약 검증을 하나의 되돌릴 수 있는 커밋으로 남긴다.
