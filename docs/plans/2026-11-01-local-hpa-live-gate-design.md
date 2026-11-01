# Local HPA Cap Live Gate Design

## Goal

단일 노드 OrbStack에서 `live-release-gate.sh`를 실행할 때만 user-service와 notification-service의 HPA 상한을 임시로 2로 제한하고, gate의 성공·실패·중단 뒤 원래 값을 자동 복구한다.

## Decision

명시적 opt-in 명령을 사용한다.

```bash
bash scripts/release/live-release-gate.sh \
  --local-hpa-cap --context orbstack
```

일반 `bash scripts/release/live-release-gate.sh`의 동작과 현재 kubectl context 사용 방식은 유지한다. `--local-hpa-cap`은 `--context` 없이는 실행되지 않으므로, 원격 또는 상용 cluster의 HPA를 우발적으로 제한하지 않는다.

## Architecture

`live-release-gate.sh`는 `--context NAME`을 선택적으로 받아 모든 `kubectl` 호출에 동일한 context를 적용한다. `--local-hpa-cap`과 함께 쓰이면 첫 번째 gate 프로세스는 `scripts/with-local-hpa-cap.sh --context NAME --namespace "$NAMESPACE"`로 자신을 한 번만 감싼다.

래퍼가 실행하는 내부 gate 프로세스는 `LOCAL_HPA_CAP_ACTIVE=1` 환경 표식을 전달받는다. 표식이 있는 내부 프로세스는 다시 래퍼를 시작하지 않고, 명시 context로 rollout·Ingress·NetworkPolicy 검증을 수행한다. HPA와 Deployment의 기록·제한·복구는 기존 래퍼만 책임지고, live gate는 기존 debug Pod cleanup 책임만 유지한다.

흐름은 다음과 같다.

1. 사용자가 `--local-hpa-cap --context orbstack`으로 gate를 시작한다.
2. 외부 gate가 필수 입력과 context를 검증하고 기존 HPA 래퍼를 `exec`한다.
3. 래퍼가 두 HPA 최대치와 Deployment desired replica를 기록하고 필요한 값만 2로 낮춘다.
4. 내부 gate가 동일한 `orbstack` context에서 rollout, TLS, HTTP redirect, NetworkPolicy 검증을 실행한다.
5. 내부 gate 종료 상태와 관계없이 래퍼 trap이 원래 HPA 상한과 Deployment desired replica를 복구한다.

## CLI Contract

| 호출 | 결과 |
|---|---|
| `live-release-gate.sh` | 기존 동작을 유지한다. 현재 kubectl context를 사용한다. |
| `live-release-gate.sh --context NAME` | 모든 kubectl 호출을 NAME context로 고정한다. HPA는 제한하지 않는다. |
| `live-release-gate.sh --local-hpa-cap --context NAME` | NAME context에서 HPA 제한 래퍼로 gate 전체를 감싼다. |
| `live-release-gate.sh --local-hpa-cap` | usage와 exit 2로 거부한다. |
| `live-release-gate.sh --local-hpa-cap --context NAME`와 `LOCAL_HPA_CAP_ACTIVE=1` | 내부 실행 경로다. 중첩 래핑 없이 gate 본문을 실행한다. |

`--context`는 HPA 래퍼와 live gate 양쪽에 같은 값으로 전달한다. `NAMESPACE`는 기존 환경변수를 유지하고 래퍼의 `--namespace`에도 전달한다.

## Safety

- HPA 제한은 `--local-hpa-cap`의 명시적 opt-in에서만 발생한다.
- `--local-hpa-cap`은 `--context` 없이는 kubectl 변경 전에 실패한다.
- 일반 gate, `--context`만 사용한 gate, 상용 실행 절차는 HPA를 변경하지 않는다.
- HPA의 최소 replica, CPU target, 저장소 매니페스트는 변경하지 않는다.
- 내부 gate와 래퍼가 같은 context를 사용하므로 다른 cluster에 HPA patch와 검증이 분리되지 않는다.
- 래퍼의 `EXIT`, `INT`, `TERM` trap이 이미 검증된 복구 단일 책임으로 유지된다.

## Test Strategy

새 `scripts/test/live-release-gate-test.sh`는 가짜 `kubectl`, `curl`, HPA 래퍼를 `PATH` 앞에 두고 다음을 검증한다.

1. 기본 gate는 HPA 래퍼를 호출하지 않는다.
2. `--context fake`는 모든 kubectl 호출에 `--context fake`를 포함한다.
3. `--local-hpa-cap`만 준 호출은 wrapper와 kubectl 변경 전에 exit 2로 실패한다.
4. `--local-hpa-cap --context fake`는 `with-local-hpa-cap.sh --context fake --namespace notification-hub`를 한 번만 호출하고, 내부 gate에 같은 context를 전달한다.
5. `LOCAL_HPA_CAP_ACTIVE=1` 내부 실행은 wrapper를 다시 호출하지 않는다.

기존 `scripts/test/with-local-hpa-cap-test.sh`는 HPA 자체의 변경·복구 계약을 계속 검증한다.

## Non-goals

- 상용 cluster의 HPA 자동 제한.
- HPA 대상, cap 값, namespace의 동적 구성.
- live gate의 rollback, backup restore, 실제 provider 발송 자동화.
- 실패한 application Deployment의 원인 수정.

## Verification

1. `bash scripts/test/with-local-hpa-cap-test.sh`
2. `bash scripts/test/live-release-gate-test.sh`
3. `bash -n scripts/with-local-hpa-cap.sh scripts/release/live-release-gate.sh scripts/test/*.sh`
4. 로컬 OrbStack에서 `INGRESS_ADDRESS`, NodePort, CA bundle, `ROLLOUT_TIMEOUT=600s`를 설정한 opt-in gate를 실행한다.
5. 종료 뒤 HPA 최대치와 Deployment desired replica가 시작 시 값으로 복구됐는지 확인한다.
