# Context Notes

## 2026-07-08

- SendGrid is confirmed as the first real email provider.
- Current `delivery-service` already has Kafka consumption, `DeliveryLog`, retry/circuit breaker annotations, result publishing, and delivery idempotency.
- Phase 1 is intentionally limited to splitting the EMAIL sender boundary. It does not call SendGrid yet.
- `ChannelDelivererPort` remains unchanged so the application service contract and existing tests stay stable.
- SMS and PUSH remain log stubs until their own provider phases.
- RED verification for Phase 1 failed at test compilation because `EmailSender` did not exist, which is the intended missing boundary.
- Mockito inline mock maker failed on the local JDK agent attach mechanism, so `ChannelDelivererAdapterTest` uses a small manual recording test double instead.
- `LoggingEmailSender` is a temporary Spring bean that preserves current EMAIL log behavior until the SendGrid sender replaces it.
- Existing delivery-service Mockito tests also hit the same inline mock maker issue. The service now opts into Mockito's subclass mock maker under test resources because these tests only mock interfaces.
- Phase 2 will add environment-backed settings only. It will not fail startup when `SENDGRID_API_KEY` is absent because the app still defaults to the logging sender until Phase 3.
- `email.provider` defaults to `logging`. `sendgrid.api-key` and `sendgrid.from-email` default to empty strings to keep local startup working before SendGrid is explicitly enabled.
- Phase 3 uses SendGrid's Mail Send endpoint with `POST /v3/mail/send`, bearer auth, `personalizations`, `from`, `subject`, and `text/plain` content. A `202 Accepted` response is treated as success.
- `sendgrid.subject` will be added with a default subject because the current `EmailSender` contract only receives recipient and body content.
- `LoggingEmailSender` is active only when `email.provider` is missing or set to `logging`; `SendGridEmailSender` is active when `email.provider=sendgrid`.
- SendGrid 4xx/5xx responses and missing required settings throw `EmailDeliveryException`, which the existing delivery flow catches and records as FAILED.
- Phase 4 focuses on provider failure coverage: client/network failures should also be wrapped as `EmailDeliveryException`, and missing `fromEmail` should be explicitly tested.
- Phase 5 updates README, PROCESS, and `delivery-service/DELIVERY-SERVICE-FLOW.md` so EMAIL is documented as SendGrid-capable while SMS/PUSH remain stubs.
- Full `mvn test` initially failed in user-service because Mockito inline mock maker cannot self-attach on the local JDK. The same subclass mock maker setting used in delivery-service is being applied to user, notification, and analytics tests.
- Full `mvn test` then passed across all modules: user 21, notification 13, delivery 21, analytics 18.
- SendGrid manual verification initially returned `403` because the sender address was not verified. After completing Sender Identity verification and refreshing `.env.local`, SendGrid returned `202 Accepted` and the recipient mailbox received the test email.

## 2026-07-12

- SMS/PUSH delivery will follow the EMAIL provider pattern instead of changing `ProcessDeliveryService` or `ChannelDelivererPort`.
- SMS 1차 Provider is Twilio because the current delivery contract already has a single recipient string that maps cleanly to a phone number.
- PUSH 1차 target is Android FCM. iOS is intentionally deferred because iOS delivery requires FCM plus APNs authentication setup in Firebase.
- Android FCM 1차 keeps the contract simple: `recipient` is treated as an FCM registration token and `content` is treated as the notification body.
- SMS/PUSH local defaults stay on `logging` so developers can run delivery-service without Twilio or Firebase secrets.
- Twilio and FCM credentials must remain environment-backed and must not be committed to source control.
- Provider-specific sender exceptions should flow into the existing delivery failure path so `DeliveryLog FAILED` and failure `DeliveryResultEvent` behavior remains consistent across EMAIL, SMS, and PUSH.

## 2026-07-13

- Phase 1 splits SMS/PUSH sender boundaries only. It deliberately does not add Twilio or FCM API calls yet.
- `ChannelDelivererAdapterTest` continues using manual recording doubles instead of Mockito to avoid the local JDK inline mock maker issue.
- RED verification failed at test compilation because `SmsSender`, `PushSender`, and the three-sender `ChannelDelivererAdapter` constructor did not exist, which matched the intended missing boundary.
- `LoggingSmsSender` and `LoggingPushSender` preserve the previous log-only behavior behind provider-specific sender interfaces.

## 2026-07-14

- Phase 2 adds configuration binding only. Twilio API calls, credential validation, and SMS send error handling stay in Phase 3.
- `SmsDeliveryProperties` mirrors `EmailDeliveryProperties` so `SMS_PROVIDER` can select `logging` or `twilio`.
- `TwilioProperties` stores account SID, auth token, from number, Messaging Service SID, and API URL with empty secret defaults to keep local startup working.
- The Twilio API URL default is `https://api.twilio.com/2010-04-01`; the concrete Messages endpoint will be composed in the Twilio sender phase.

## 2026-07-15

- Phase 3 implements `TwilioSmsSender` using Twilio's Messages API path `/Accounts/{AccountSid}/Messages.json`.
- Twilio requests use HTTP Basic Auth with Account SID and Auth Token and send form-urlencoded fields `To`, `Body`, and either `From` or `MessagingServiceSid`.
- `TwilioSmsSender` treats `201 Created` as success and wraps provider responses and network failures in `SmsDeliveryException`.
- Required setting validation happens before the request: account SID, auth token, and either from number or Messaging Service SID.
- Actual Twilio SMS delivery is not manually verified yet because it requires real Twilio credentials and a test recipient phone number.

## 2026-07-16

- Phase 4 adds Android FCM configuration binding only. FCM HTTP v1 calls, service account token creation, and provider error handling stay in Phase 5.
- `PushDeliveryProperties` mirrors `EmailDeliveryProperties` and `SmsDeliveryProperties` so `PUSH_PROVIDER` can select `logging` or `fcm`.
- `FcmProperties` stores Firebase project id, service account JSON, service account path, FCM API URL, and default notification title.
- `GOOGLE_APPLICATION_CREDENTIALS` is mapped into `fcm.credentials-path` to match common Firebase service account usage without committing credential files.
- PUSH 1차 keeps the simple contract from the design: `recipient` is an Android FCM registration token.

## 2026-07-17

- Phase 5 implements Android FCM HTTP v1 sender using `POST /v1/projects/{projectId}/messages:send`.
- `FcmPushSender` sends `recipient` as `message.token`, `fcm.title` as `notification.title`, and `content` as `notification.body`.
- `FcmAccessTokenProvider` separates OAuth access token creation from request construction so sender tests do not need real Firebase credentials.
- `GoogleServiceAccountAccessTokenProvider` uses Google Auth Library with the `https://www.googleapis.com/auth/firebase.messaging` scope.
- Token provider credential lookup order is inline service account JSON, service account file path, then Application Default Credentials.
- `GoogleServiceAccountAccessTokenProvider` caches scoped credentials and uses `refreshIfExpired()` so each push send does not force a token refresh request.
- Added `google-auth-library-oauth2-http` because FCM HTTP v1 requests require OAuth 2.0 bearer tokens.
- Actual Android FCM delivery is not manually verified yet because it requires a Firebase project, service account, and Android registration token.

## 2026-07-18

- Phase 6 reviewed `ProcessDeliveryServiceTest` before adding any new tests.
- `ProcessDeliveryService` catches exceptions from `ChannelDelivererPort.deliver()` after the channel-specific sender layer, so EMAIL, SMS, and PUSH provider failures share the same failure path.
- The existing `process_deliveryFails_savesFailedAndPublishes` test already verifies that a provider exception stores `DeliveryLog FAILED` and publishes a failure `DeliveryResultEvent`.
- No SMS/PUSH-specific failure-flow application test was added because it would duplicate the same channel-agnostic behavior without increasing coverage.
- `mvn test -pl delivery-service` passed with 39 tests after Phase 6 documentation updates.
- Full multi-module `mvn test` passed after Phase 6 documentation updates. Verified counts are user 21, notification 13, delivery 39, and analytics 18.
- README, PROCESS, delivery-service flow, and manual test docs now document SendGrid, Twilio, and Android FCM provider support separately from actual external manual verification status.

## 2026-07-19

- Phase 7 remaining work is iOS PUSH follow-up.
- Firebase iOS app registration, APNs authentication key creation, and APNs key upload are external console tasks and cannot be completed from the local repository.
- The current `FcmPushSender` uses FCM HTTP v1 `message.token` and `notification` payload, so it is platform-neutral at the backend boundary.
- iOS 1차 contract keeps `channel=PUSH`, `recipient=iOS FCM registration token`, `content=notification body`, and `FCM_TITLE=notification title`.
- No platform-specific FCM payload branch is needed before the first iOS delivery test.
- A separate iOS PUSH follow-up plan was added at `docs/plans/2026-07-19-ios-push-delivery.md`.
- `mvn test -pl delivery-service` passed with 39 tests after the Phase 7 documentation and FCM naming update.

## 2026-07-20

- Apple Developer Program paid enrollment is deferred.
- The next practical external verification target is Android FCM because it does not require Apple Developer or APNs setup.
- Firebase official docs confirm FCM HTTP v1 sends to a device registration token at `POST https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`.
- Firebase official docs recommend `GOOGLE_APPLICATION_CREDENTIALS` for local service account authorization, and the required OAuth scope is `https://www.googleapis.com/auth/firebase.messaging`.
- Android FCM actual delivery still requires a Firebase project id, service account JSON, and Android FCM registration token in local-only `.env.local`.
- Added `scripts/verify-android-fcm-env.sh` so missing local FCM settings can be checked without printing secret values.
- Preflight against the repository root `.env.local` reported missing `PUSH_PROVIDER=fcm`, `FCM_PROJECT_ID`, service account credentials, and `ANDROID_FCM_REGISTRATION_TOKEN`.
- `bash -n scripts/verify-android-fcm-env.sh` passed.
- `mvn test -pl delivery-service` passed with 39 tests after the Android FCM verification documentation update.

## 2026-07-23

- Android FCM preflight passed after `.env.local` received FCM provider, project id, service account credentials, and Android registration token.
- The first retry failed because `GOOGLE_APPLICATION_CREDENTIALS` pointed to Android `google-services.json`, which does not contain `client_email` or `private_key`.
- After switching to Firebase service account private key JSON, OAuth access token issuance succeeded.
- Direct FCM HTTP v1 send returned `200 OK` and a message name for project `notification-hub-c680b`.
- Android device receipt still needs user-side confirmation.
- Added `.gitignore` patterns for Firebase admin SDK and service account JSON files because a private key JSON was placed inside the repository path.

## 2026-07-24

- Sent a direct Android FCM notification with body `안녕?울트라?`.
- OAuth access token issuance succeeded.
- FCM HTTP v1 send returned `200 OK` and message name `projects/notification-hub-c680b/messages/0:1784885378031120%54b6692d54b6692d`.
- User confirmed the Android device received the `안녕?울트라?` notification.

## 2026-08-02

- Reviewed documentation against the current code and test state before the next improvement phase.
- Full multi-module `mvn test` passed with 91 tests: user 21, notification 13, delivery 39, analytics 18.
- Several P2 items in `docs/improvement-todo.md` were already implemented after the original March review: Redis counter parse safety, UTC time usage, notification content length limit, delivery transaction boundary, and gateway JWT exception handling.
- Android FCM server-routed delivery was verified on 2026-08-01 through `notification-service` -> Kafka -> `delivery-service`, with delivery status `SUCCESS` and one attempt.
- Remaining high-priority improvements are gateway fallback handling, direct-service-call security policy, and pageable list APIs.

## 2026-08-10

- The Android test app now treats data-only FCM payloads as the standard path for in-app log verification.
- This server repo still documents an Android direct FCM HTTP v1 example with `notification` payload, which can bypass the Android app's storage path when the app is backgrounded.
- The backend `FcmPushSender` currently sends `message.notification.title` and `message.notification.body`. That remains the delivery-service provider contract for this documentation pass.
- The documentation update separates two paths: direct data-only FCM HTTP v1 calls for `android_noti_app` log/UI verification, and delivery-service routed notification PUSH for backend provider integration verification.
- `manual_test.md` Android direct FCM HTTP v1 example now sends `message.data.title`, `message.data.body`, and `message.data.source`.
- README and `delivery-service/DELIVERY-SERVICE-FLOW.md` now state that delivery-service routed PUSH still uses notification payload, while Android app log UI verification should use the direct data-only call.

## 2026-08-11

- The next local high-priority improvement is API Gateway fallback handling from P2 #20 in `docs/improvement-todo.md`.
- `api-gateway/src/main/resources/application.yml` already forwards Circuit Breaker fallback requests to `/fallback`.
- There is currently no `/fallback` controller in api-gateway.
- The implementation will add a small reactive controller that returns HTTP 503 with `ApiResponse.error("Service temporarily unavailable")`.
- The first focused fallback test failed as expected because `FallbackController` did not exist.
- After adding `FallbackController`, the same test returned 401 because `common` brings Spring Security onto the gateway classpath and default WebFlux security intercepted `/fallback`.
- Added `FallbackResponseFilter` for the `/fallback` path only, so fallback responses are written before default security interception without changing the gateway's broader security chain.
- Focused verification passed with `mvn test -pl api-gateway -am -Dtest=FallbackControllerTest -Dsurefire.failIfNoSpecifiedTests=false`.

## 2026-08-12

- The next selected P2 item is analytics #23, `findByTenantId()` pagination.
- The current `DeliveryEventRepository.findByTenantId(String)` returns `List<DeliveryEvent>`, and `DeliveryEventMongoRepository.findByTenantId(String)` returns all matching documents.
- The minimal approved scope is repository-level pagination only: no new REST API, no DTO response shape, and no controller change.
- The implementation will require callers to pass `Pageable` and will preserve Spring Data `Page` metadata through the adapter.
- RED verification failed as expected because neither the domain port nor Mongo repository accepted `Pageable`.
- `DeliveryEventRepository`, `DeliveryEventMongoRepository`, and `DeliveryEventRepositoryAdapter` now use `Page<...> findByTenantId(String, Pageable)`.
- Focused verification passed with `mvn test -pl analytics-service -am -Dtest=DeliveryEventRepositoryAdapterTest -Dsurefire.failIfNoSpecifiedTests=false`.

## 2026-08-14

- The next selected P2 item is user #27, `Role "ADMIN"` hardcoding.
- The minimal approved scope keeps `User.role` as `String` so JPA mapping, JWT claims, mappers, and public contracts do not change.
- The implementation will add a domain `UserRole` enum and have `User.create()` use `UserRole.ADMIN.name()` for the existing default role value.
- RED verification failed as expected because `UserRole` did not exist.
- Added `UserRole.ADMIN` and replaced the default role literal in `User.create()` with `UserRole.ADMIN.name()`.
- Focused verification passed with `mvn test -pl user-service -am -Dtest=UserTest -Dsurefire.failIfNoSpecifiedTests=false`.

## 2026-08-15

- The next selected P2 item is user #22, password complexity validation.
- `RegisterTenantRequest.password` already has `@Size(min = 8, max = 100)` and a `@Pattern` requiring uppercase, lowercase, number, and one `@$!%*?&` special character.
- The approved scope is to add DTO Bean Validation tests and update status docs only, with no production code change.
- Added `RegisterTenantRequestTest` to verify valid complex passwords pass and passwords missing uppercase, lowercase, number, or special character fail validation.
- Focused verification passed with `mvn test -pl user-service -am -Dtest=RegisterTenantRequestTest -Dsurefire.failIfNoSpecifiedTests=false`.

## 2026-08-16

- The next selected P2 item is api-gateway #19, IP-only Rate Limiting.
- The minimal scope keeps the existing Redis token bucket values and only changes the key selection strategy.
- Protected requests should be rate limited by JWT-derived `X-Tenant-Id`, because `JwtAuthenticationFilter` removes client-supplied tenant headers before reinjecting trusted claims.
- Requests without a tenant header should fall back to the first `X-Forwarded-For` IP, then remote address, then `unknown`.
- Added `tenantRateLimitKeyResolver` and wired `RequestRateLimiter` to user, notification, delivery, and analytics routes.
- Focused verification passed with `mvn test -pl api-gateway -am -Dtest=GatewayConfigTest -Dsurefire.failIfNoSpecifiedTests=false`.
- Full api-gateway verification initially failed in the existing `FallbackControllerTest` because api-gateway lacked the Mockito subclass mock maker test setting already used by user, notification, delivery, and analytics modules.
- Added the same Mockito subclass mock maker setting to api-gateway test resources.
- Full multi-module `mvn test` passed with 103 tests: api-gateway 5, user 27, notification 13, delivery 39, analytics 19.
- Manual configuration surface check confirmed `application.yml` parses and every `RequestRateLimiter` route references `tenantRateLimitKeyResolver`.

## 2026-08-17

- The next selected local P2 item is analytics #18, `DailyStats` internal `long[]` channel counters.
- `ChannelStats` already exists and is returned by `DailyStats.getChannelStats()`, so adding a separate `ChannelCounter` type would duplicate the same domain concept.
- The minimal scope is to store `Map<String, ChannelStats>` inside `DailyStats` while keeping `DailyStatsDocument` as `Map<String, long[]>` for the existing MongoDB shape and atomic `$inc` paths.
- RED verification failed as expected because `DailyStats.reconstruct()` still required `Map<String, long[]>`.
- `DailyStats` now stores `Map<String, ChannelStats>` internally, and `DailyStatsDocument` converts between the domain value and the existing Mongo `long[]` shape.
- Focused verification passed with `mvn test -pl analytics-service -am -Dtest=DailyStatsTest -Dsurefire.failIfNoSpecifiedTests=false`.
- Analytics module verification passed with `mvn test -pl analytics-service -am` and 20 tests.
- Full multi-module `mvn test` passed with 104 tests: api-gateway 5, user 27, notification 13, delivery 39, analytics 20.

## 2026-08-19

- Reviewed commercialization readiness separately from the earlier portfolio-oriented improvement list.
- The highest-priority commercialization item is blocking direct calls to internal services, because user, notification, delivery, and analytics `SecurityConfig` still use `permitAll()`.
- The planned approach is defense in depth: service-level JWT validation for protected APIs plus K8s NetworkPolicy so only api-gateway and approved monitoring Pods can reach internal service Pods.
- The user asked not to proceed with iOS work, so iOS Firebase/APNs console tasks remain intentionally untouched.
- Internal service access control Task 1 is limited to API Gateway route policy cleanup.
- The current gateway config exposed `/api/users/**, /api/keys/**` through one user-service route without `JwtAuthentication`.
- Added a route config test that requires separate `user-service-register`, `user-service-auth`, and `user-service-api-keys` routes.
- `/api/users/register` remains public but keeps `RequestRateLimiter`, so unauthenticated registration traffic is limited by the resolver's IP fallback.
- `/api/keys/**` now runs `JwtAuthentication` before `RequestRateLimiter`, so the rate limit key can use trusted JWT-derived `X-Tenant-Id`.
- Task 2 adds the reusable Servlet-side JWT filter in `common`.
- The filter must reject missing or invalid bearer tokens, bypass configured public paths, overwrite forged tenant/user headers with JWT claims, and populate `SecurityContext` so service `SecurityConfig` can use `authenticated()`.

## 2026-08-20

- Implemented `JwtHeaderAuthenticationFilter` in `common` as the reusable Servlet-side direct-call guard for internal services.
- The filter uses `JwtTokenProvider`, rejects missing or invalid bearer tokens with 401, and bypasses `JwtSecurityProperties.excludedPaths()`.
- Valid requests are forwarded through a request wrapper that returns JWT-derived `X-Tenant-Id` and `X-User-Id`, so forged inbound headers are overwritten before controller code sees them.
- The filter also populates `SecurityContext` with the JWT subject as principal, which prepares Task 3 service `SecurityConfig` changes to use `authenticated()`.
- Task 3 still needs to register this filter in user, notification, delivery, and analytics service SecurityConfig classes.
- Task 3 will register the common Servlet JWT filter in user, notification, delivery, and analytics services.
- User-service public paths remain `/api/users/register`, `/api/auth/**`, and actuator health endpoints.
- Notification, delivery, and analytics services expose only actuator health endpoints publicly; service API paths require JWT.
- Security tests use each real service `SecurityConfig` with test-only probe controllers to verify direct protected calls return 401 and JWT calls receive trusted tenant headers.

## 2026-08-21

- Task 3 applies the common Servlet JWT filter to user, notification, delivery, and analytics service `SecurityConfig` classes.
- RED service security tests failed as expected because protected probe APIs still returned 200 without a token and preserved forged `X-Tenant-Id` headers.
- Each service now registers `JwtHeaderAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`, disables session state, and requires authentication for service APIs.
- User-service keeps `/api/users/register`, `/api/auth/**`, and actuator health endpoints public.
- Notification, delivery, and analytics services keep only actuator health endpoints public.
- Each service `application.yml` now sets `jwt.security.excluded-paths` so the filter and `SecurityFilterChain` public path policy stay aligned outside tests.
- Focused verification passed with `mvn test -pl user-service,notification-service,delivery-service,analytics-service -am -Dtest=SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false`.
- K8s NetworkPolicy remains the next defense-in-depth step for the internal service access control plan.

## 2026-08-22

- Task 4 adds Kubernetes NetworkPolicy as the network-level guard for internal services.
- The policy selects `user-service`, `notification-service`, `delivery-service`, and `analytics-service` Pods by their existing `app` labels and applies ingress default deny.
- Service API ingress is allowed only from Pods with `app=api-gateway` in the same `notification-hub` namespace.
- Actuator ingress is allowed only from Prometheus Pods labeled `app.kubernetes.io/name=prometheus` in a namespace named `monitoring` or labeled `monitoring=true`.
- The existing local Docker Prometheus workflow uses `kubectl port-forward`; that path is not represented as in-cluster Pod-to-Pod traffic and is documented separately from the NetworkPolicy allow list.
- `kubectl apply --dry-run=client -f k8s/networkpolicy/` could not complete because the current `orbstack` Kubernetes API at `127.0.0.1:26443` refused connections.
- Local structural validation passed with Ruby YAML parsing and NetworkPolicy field checks for all 9 documents.

## 2026-08-23

- The next commercialization P0 item is K8s Secret plaintext removal.
- `k8s/secret.yaml` contained tracked DB password and JWT secret values, so it is removed from Git and added to `.gitignore`.
- `k8s/secret.example.yaml` now documents the required Secret keys with placeholder values only.
- Runtime `notification-hub-secret` creation is moved to the deployment procedure using `kubectl create secret generic ... --dry-run=client -o yaml | kubectl apply -f -`.
- MySQL and MongoDB infra deployments now read password values from `notification-hub-secret` instead of YAML literals.
- README deployment order now creates namespace, ConfigMap, and Secret before deploying infra Pods, because MySQL and MongoDB depend on the Secret at startup.

## 2026-08-24

- The next commercialization P0 item is DB migration system introduction.
- Flyway is the smallest fit because Spring Boot auto-configures it from the classpath and each JPA service can own its `db/migration` scripts.
- The scope is limited to MySQL-backed user, notification, and delivery services. Analytics remains MongoDB-based and is not changed here.
- New empty DBs should be created at database and grant level by `docker/mysql/init.sql` or K8s MySQL init SQL, then service startup applies Flyway V1 and Hibernate validates the schema.
- Existing DBs previously created by Hibernate `ddl-auto=update` must be inspected before rollout. A one-time `FLYWAY_BASELINE_ON_MIGRATE=true` baseline may be used only after confirming the schema matches the application.
- MySQL smoke verification found Hibernate 6 expects native MySQL `ENUM` for fields annotated with `@Enumerated(EnumType.STRING)`, while plain `String` fields such as `UserEntity.role` must remain `VARCHAR(255)`.
- After adjusting the V1 SQL, Flyway applied all three service migrations to a temporary MySQL 8.0.44 container and user, notification, and delivery services all reached Hibernate schema validation successfully.

## 2026-08-25

- The next commercialization P0 item is core E2E integration test coverage.
- The approved scope is option 1: split the E2E surface into a notification acceptance test and a delivery plus analytics pipeline test.
- A dedicated `e2e-tests` Maven module keeps Docker-backed integration tests visible and avoids coupling them to production service modules.
- The notification acceptance test should use real MySQL, Redis, and Kafka containers and verify Flyway schema, notification persistence, Redis idempotency, and Kafka event publication.
- The delivery plus analytics pipeline test should use real Kafka, MySQL, MongoDB, and Redis containers and verify a `NotificationEvent` becomes a successful delivery log, an analytics delivery event, and a Redis realtime counter increment.
- Spring Boot official docs support `@ServiceConnection` and `@DynamicPropertySource` for Testcontainers connection details. This project needs explicit dynamic properties because multiple service contexts share the same container set.
- Testcontainers official docs support JUnit Jupiter integration and module dependencies for MySQL, Kafka, and MongoDB. Redis will use a `GenericContainer` with the core Testcontainers dependency.
- Docker-dependent tests should use `@Testcontainers(disabledWithoutDocker = true)` so local or CI environments without Docker skip these tests instead of failing unrelated builds.
- Added `NotificationAcceptanceIntegrationTest` to verify notification-service writes MySQL state, stores the Redis idempotency key, and publishes the `NotificationEvent` to Kafka with real containers.
- Added `DeliveryAnalyticsPipelineIntegrationTest` to verify a Kafka `NotificationEvent` becomes a successful delivery log, a Mongo analytics delivery event, and a Redis realtime counter increment.
- Local Docker Engine 29 through OrbStack requires Docker API 1.40 or newer, while the resolved Testcontainers client defaulted unknown Docker API versions to 1.32. The `e2e-tests` Surefire config now sets `api.version=1.44`.
- E2E startup exposed Flyway V1 collisions because the dedicated test module depends on multiple service modules. E2E contexts now pin `spring.flyway.locations` to each service's migration directory.
- E2E schema validation exposed lowercase MySQL ENUM definitions for Java `EnumType.STRING` fields. Notification and delivery V1 migrations now use the uppercase Java enum names.
- Delivery pipeline E2E exposed a same-transaction JPA merge issue when a delivery log is saved as PENDING and then SUCCESS with the same ID. `DeliveryLogRepositoryAdapter.save()` now updates the managed entity when the log already exists.
- Verification passed with `mvn test -pl e2e-tests -am`: E2E 2 tests, failures 0, errors 0, skipped 0.
- Full reactor verification passed with `mvn test`: 9 Maven modules, including Docker-backed E2E tests.

## 2026-08-18

- The next selected P2 item is #14, JPA `@Version` optimistic locking.
- The minimal scope is persistence entities only: add `@Version Long version` without changing domain models, mappers, DTOs, or API responses.
- Target JPA entities are `TenantEntity`, `UserEntity`, `ApiKeyEntity`, `NotificationEntity`, and `DeliveryLogEntity`.
- Reflection-based structure tests will verify the field exists without requiring a database.
- RED verification failed as expected because the user entity structure test could not find a `version` field.
- Added `@Version Long version` to the five JPA entities while leaving constructors and mapper contracts unchanged.
- Focused verification passed with `mvn test -pl user-service,notification-service,delivery-service -am -Dtest=JpaOptimisticLockingTest -Dsurefire.failIfNoSpecifiedTests=false`.
- Target JPA module verification passed with `mvn test -pl user-service,notification-service,delivery-service -am`: user 28, notification 14, delivery 40.
- Full multi-module `mvn test` passed with 107 tests: api-gateway 5, user 28, notification 14, delivery 40, analytics 20.

## 2026-08-26

- The next commercialization P1 item is DLQ operator tooling.
- This branch is stacked on `origin/feat/e2e-integration-tests` because the E2E work is pushed but not merged into `main`.
- Baseline verification passed in the stacked worktree with `mvn test`: 9 Maven modules, including Docker-backed E2E tests.
- The selected approach is a dedicated `dlq-ops` Maven module instead of shell scripts or an API surface.
- `dlq-ops` will reuse `common` `NotificationEvent` and Spring Kafka serializers so operator replay uses the same event contract as the services.
- The CLI starts with `list`, `export`, and `replay`. Replay defaults to dry-run and requires `--execute` for actual Kafka publication.
- DLQ reads should use `enable.auto.commit=false` and a generated group id by default, so inspecting DLQ messages does not advance an operator-managed offset.
- Export format is JSON Lines containing Kafka metadata plus `NotificationEvent`, which makes replay auditable and reviewable before execution.
- Task 1 RED verification failed as expected because Maven could not find `dlq-ops` in the reactor.
- Added the `dlq-ops` Maven module with a minimal `DlqOpsApplication` that supports `--help`, missing command errors, and unknown command errors.
- Focused verification passed with `mvn test -pl dlq-ops -am -Dtest=DlqOpsApplicationTest -Dsurefire.failIfNoSpecifiedTests=false`: 2 tests, failures 0, errors 0.
- Manual CLI surface verification passed for `java -cp dlq-ops/target/classes com.notificationhub.dlqops.DlqOpsApplication --help` with exit code 0.
- Manual invalid-command verification returned exit code 1 and printed `Unknown command 'nope'. Run with --help.`
- Full reactor verification passed with `mvn test`: 10 Maven modules, including `dlq-ops` and Docker-backed E2E tests.
- Task 2 RED verification failed as expected because `DlqOptions`, `DlqOptionsParser`, `DlqCommand`, and `DlqEventFilter` did not exist.
- Added manual option parsing with defaults for local Kafka, DLQ source topic, replay target topic, generated group id, limit, timeout, filters, file paths, and `--execute`.
- Added event filtering for tenant id, notification id, and channel.
- Integrated parser validation into `DlqOpsApplication` so unknown options fail before command execution.
- Focused verification passed with `mvn test -pl dlq-ops -am -Dtest=DlqOptionsParserTest,DlqEventFilterTest,DlqOpsApplicationTest -Dsurefire.failIfNoSpecifiedTests=false`: 11 tests, failures 0, errors 0.
- Module verification passed with `mvn test -pl dlq-ops -am`: common 4 tests and dlq-ops 11 tests.
- Task 3 RED verification failed as expected because `DlqRecord` and `DlqRecordCodec` did not exist.
- Added `DlqRecord` to preserve Kafka topic, partition, offset, timestamp, key, and `NotificationEvent` in export files.
- Added `DlqRecordCodec` with Jackson Java Time support and JSON Lines file read/write methods.
- Focused verification passed with `mvn test -pl dlq-ops -am -Dtest=DlqRecordCodecTest -Dsurefire.failIfNoSpecifiedTests=false`: 3 tests, failures 0, errors 0.
- Module verification passed with `mvn test -pl dlq-ops -am`: common 4 tests and dlq-ops 14 tests.
- Task 4 RED verification failed as expected because `DlqConsumerClient` did not exist.
- Added `DlqConsumerClient` as the list/export read boundary and `KafkaDlqConsumerClient` as the Kafka implementation.
- `KafkaDlqConsumerClient` uses `enable.auto.commit=false`, `auto.offset.reset=earliest`, String keys, and Spring Kafka JSON deserialization for `NotificationEvent`.
- `DlqOpsApplication` now executes `list` and `export`, applies `DlqEventFilter`, respects `limit`, and requires `--output` for export.
- Focused verification passed with `mvn test -pl dlq-ops -am -Dtest=DlqOpsListExportTest -Dsurefire.failIfNoSpecifiedTests=false`: 3 tests, failures 0, errors 0.
- Module verification passed with `mvn test -pl dlq-ops -am`: common 4 tests and dlq-ops 17 tests.
- Task 5 RED verification failed as expected because `DlqReplayClient` and the replay-aware `DlqOpsApplication.run(...)` overload did not exist.
- Added `DlqReplayClient` as the replay publish boundary and `KafkaDlqReplayClient` as the Kafka producer implementation.
- `replay` reads JSON Lines export files, applies the same filters and limit, defaults to dry-run, and only publishes when `--execute` is present.
- `KafkaDlqReplayClient` publishes all replay records through one producer instance and uses the original Kafka key when present, otherwise `event.notificationId()`.
- Focused verification passed with `mvn test -pl dlq-ops -am -Dtest=DlqOpsReplayTest -Dsurefire.failIfNoSpecifiedTests=false`: 3 tests, failures 0, errors 0.
- Module verification passed with `mvn test -pl dlq-ops -am`: common 4 tests and dlq-ops 20 tests.
- The final operator surface is documented as an executable jar built by `mvn -pl dlq-ops -am package`.
- README, `manual_test.md`, and `docs/kafka-redis.md` now show `list`, `export`, dry-run `replay`, and `replay --execute` examples.
- The commercialization priority list now points to P1 6, alerting and alarm rules, as the next item.
- Packaging verification passed with `mvn -pl dlq-ops -am package`; Spring Boot repackage produced `dlq-ops-1.0.0-SNAPSHOT.jar`.
- Manual jar help verification passed with `java -jar dlq-ops/target/dlq-ops-1.0.0-SNAPSHOT.jar --help`, exit code 0.
- Manual invalid-command verification passed with `java -jar dlq-ops/target/dlq-ops-1.0.0-SNAPSHOT.jar nope`, exit code 1.
- Manual dry-run replay verification passed with `java -jar dlq-ops/target/dlq-ops-1.0.0-SNAPSHOT.jar replay --input /private/tmp/dlq-ops-smoke.jsonl`, exit code 0 and one replay candidate.
- Final full reactor verification passed with `mvn test`: 10 Maven modules, including `dlq-ops` and Docker-backed E2E tests.

## 2026-08-26 CI follow-up

- PR #1 CI failed in `mvn clean verify` during E2E test compilation because the three service dependencies were declared with `test` scope and their classes were absent from the clean reactor test classpath.
- The E2E module contains only integration tests, so notification, delivery, and analytics service dependencies use the default compile scope to make their application classes available when compiling those tests.
- A clean `verify` still failed because Spring Boot repackage replaced each service's main artifact with a fat JAR whose classes are nested under `BOOT-INF/classes`. Service packaging now keeps the plain JAR as the main artifact and attaches the executable JAR with the `exec` classifier; Dockerfiles copy only the executable classifier.
- Verification after the packaging change passed for `mvn clean package -DskipTests -pl notification-service,delivery-service,analytics-service -am`, including both plain and `-exec.jar` artifacts. E2E reports also passed with 2 tests, 0 failures, 0 errors, and 0 skipped after `mvn -pl e2e-tests -am test`.

## 2026-08-27 Alerting design

- The next commercialization item is P1 6, alerting and alarm rules.
- The approved delivery scope is Webhook and SMTP email at the same time.
- Alertmanager will use independent Webhook and email receivers under one default route, with grouped alerts, repeat suppression, and resolved notifications.
- Prometheus rules will cover service down and HTTP 5xx first. DLQ lag and Provider failure rules must use verified time series; missing application metrics will be split into a separate instrumentation task instead of being guessed.
- Credentials and endpoint values are environment-variable inputs only. No SMTP password, Webhook URL, or recipient address will be committed.
- The implementation plan is recorded in `docs/plans/2026-08-27-alerting.md`. It deliberately starts with ServiceDown and HTTP 5xx rules because those metrics are verified in the current codebase; DLQ lag and Provider failure rules require confirmed time series or a separate instrumentation change.
- Task 1 review found that discovery-service was scraped but did not expose `/actuator/prometheus`; its actuator exposure now includes `prometheus` so `ServiceDown` does not create a false critical alert for a healthy discovery service.
- Task 2 added Alertmanager Webhook and SMTP routing with grouped alerts, repeat suppression, and resolved notifications. SMTP passwords use `smtp_auth_password_file`; the entrypoint rejects YAML-breaking scalar values before starting Alertmanager.
- Task 3 connected Prometheus to Alertmanager at `alertmanager:9093`, mounts the Prometheus rule file and Alertmanager entrypoint, and separates host password source path from the container password file path.
- Alerting configuration verification passed with `docker compose config --quiet`, `sh -n monitoring/alertmanager/entrypoint.sh`, and containerized Prometheus rule/config checks. Alertmanager readiness and real Webhook/SMTP delivery remain environment-dependent because the image pull timed out and no external credentials are available.
