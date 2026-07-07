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
