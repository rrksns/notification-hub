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
