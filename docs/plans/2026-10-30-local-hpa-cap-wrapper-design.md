# Local HPA Cap Wrapper Design

## Goal

단일 노드 로컬 Kubernetes 환경에서 Docker Testcontainers와 JVM Pod가 자원을 경쟁하지 않도록, 검증 명령 전후에 `user-service-hpa`와 `notification-service-hpa`의 최대 replica를 임시로 2로 제한하고 항상 원래 상태로 복구한다.

## Decision

`scripts/with-local-hpa-cap.sh`를 범용 Bash 래퍼로 추가한다.

```bash
./scripts/with-local-hpa-cap.sh --context orbstack -- mvn test
```

래퍼는 검증 명령을 해석하거나 Maven에 종속되지 않는다. `--` 뒤의 명령을 그대로 실행하므로 Testcontainers E2E, Maven 모듈 테스트, live release gate 등 같은 로컬 자원 경합 문제를 가진 검증에 재사용할 수 있다.

`mvn test` 전용 스크립트는 다른 검증 경로를 다시 구현해야 하고, Makefile 목표는 현재 저장소의 스크립트 관례에 새 진입점을 추가하므로 선택하지 않는다.

## Scope

고정 대상은 `notification-hub` namespace의 아래 두 HPA와 같은 이름의 Deployment다.

- `user-service-hpa`와 `user-service`
- `notification-service-hpa`와 `notification-service`

스크립트는 다음 순서로 동작한다.

1. `--context`를 필수로 받고 namespace는 `--namespace` 또는 기본값 `notification-hub`를 사용한다.
2. `kubectl --context` 연결, namespace, 두 HPA, 두 Deployment의 존재를 읽기 전용으로 확인한다.
3. 각 HPA의 원래 `spec.maxReplicas`와 Deployment의 원래 `spec.replicas`를 메모리에 기록하고 출력한다.
4. 최대 replica가 2보다 큰 HPA만 2로 patch하고, desired replica가 2보다 큰 Deployment만 2로 scale한다.
5. `--` 뒤 검증 명령을 자식 프로세스로 실행하고 종료 상태를 보존한다.
6. `EXIT`, `INT`, `TERM` trap에서 실제로 변경한 HPA 최대치와 Deployment replica만 원래 값으로 복구한다.

HPA의 `minReplicas`와 저장소의 Kubernetes 매니페스트는 변경하지 않는다. HPA 컨트롤러가 복구 직후 실제 Pod 수를 재조정할 수 있으므로, 스크립트는 복구 요청의 성공 여부만 보장하며 최종 Pod 수를 고정하지 않는다.

## Safety and Failure Handling

- context, 대상 리소스, 검증 명령 중 하나라도 누락되면 patch 전에 non-zero로 종료한다.
- 기본 context를 사용하지 않는다. `--context orbstack`처럼 명시해야 한다.
- trap은 원래 값 기록이 끝난 뒤에만 설치한다. 초기 읽기 실패에서는 복구 명령도 실행하지 않는다.
- 복구 실패는 검증 실패와 별도로 stderr에 기록하고 non-zero 종료로 승격한다.
- `INT`와 `TERM`은 검증 자식 프로세스에 전달한 뒤 복구한다.
- 실행 로그에는 context, namespace, 변경 전후 값과 검증 종료 코드를 남기되, Secret이나 환경 변수 값은 출력하지 않는다.

## Test Strategy

새 Bash 테스트 `scripts/test/with-local-hpa-cap-test.sh`는 임시 디렉터리의 가짜 `kubectl`을 `PATH` 앞에 두고 호출을 기록한다. 실제 Kubernetes cluster를 변경하지 않는다.

필수 계약은 다음과 같다.

1. 성공하는 검증 명령은 두 HPA를 2로 제한하고, 두 Deployment를 2로 scale하며, 원래 값으로 복구한다.
2. 검증 명령이 실패해도 복구 명령이 실행되고, 래퍼는 원래 실패 상태를 반환한다.
3. 누락된 `--context`, namespace, HPA, Deployment 또는 검증 명령은 어떤 patch나 scale도 수행하지 않는다.
4. 이미 2 이하인 최대치나 replica는 변경하거나 불필요하게 scale하지 않는다.
5. `TERM`을 받은 검증도 복구 경로를 실행한다.

## Non-goals

- HPA `minReplicas`, CPU target, 매니페스트 값 변경.
- 여러 namespace나 임의 HPA를 인자로 받는 범용 운영 도구화.
- Kubernetes 리소스 잠금, 동시 실행 조정, 원격 cluster 지원.
- 검증 명령 자체의 성공 보장 또는 Docker 자원 상태 자동 복구.

## Verification

구현 시 아래 순서로 검증한다.

1. `bash scripts/test/with-local-hpa-cap-test.sh`
2. `bash -n scripts/with-local-hpa-cap.sh scripts/test/with-local-hpa-cap-test.sh`
3. 가짜 `kubectl` 테스트 로그에서 성공, 실패, 입력 검증, signal 복구 경로를 확인한다.
4. `JWT_SECRET=<base64-test-secret> ./scripts/with-local-hpa-cap.sh --context orbstack -- mvn test`를 실행한다.
