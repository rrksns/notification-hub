# Tenant Quota Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enforce monthly notification quotas from the tenant's signed subscription plan.

**Architecture:** User-service signs `SubscriptionPlan` into the access token. Gateway and servlet JWT filters propagate the verified plan as a trusted internal header. Notification-service atomically increments a Redis monthly counter and rejects the request when the plan limit is reached.

**Tech Stack:** Java 17, Spring Boot, Spring Data Redis, Redis Lua script, JJWT, JUnit 5, Mockito.

---

### Task 1: Signed plan propagation

**Files:** Modify `common` JWT provider and filters, `user-service` token port/adapter/auth service, and gateway filter tests.

1. Add the plan argument and `plan` claim to token generation.
2. Read the tenant plan during login and pass it to token generation.
3. Remove client-supplied plan headers and inject the verified claim.
4. Add failing tests, then implement and run focused tests.

### Task 2: Quota domain and Redis adapter

**Files:** Create notification quota port, properties, plan limit policy, and Redis adapter.

1. Add plan limit policy and monthly key generation.
2. Add Redis Lua atomic increment that returns the current count or rejects over-limit increments.
3. Add focused tests for limits, key period, and adapter behavior.

### Task 3: Notification creation enforcement

**Files:** Modify notification command/controller/service, common error code, and tests.

1. Carry the trusted plan into the create command.
2. Check quota after duplicate detection and before persistence.
3. Add `QUOTA_EXCEEDED` with HTTP 429 mapping.
4. Verify no repository or publisher call occurs on rejection.

### Task 4: Documentation and verification

**Files:** Modify README, checklist, context notes, manual test, and commercialization priority list.

1. Document limits, counter key, reset behavior, and 429 response.
2. Run focused and full Maven tests plus diff checks.
3. Commit, push the branch, and merge the pull request into `main`.
