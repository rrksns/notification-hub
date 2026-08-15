# User Password Complexity 설계

## 목표

`RegisterTenantRequest.password`에 이미 적용된 비밀번호 복잡도 검증을 테스트와 문서로 고정한다.

## 범위

- production validation annotation은 변경하지 않는다.
- controller, service, domain 로직은 변경하지 않는다.
- 새 비밀번호 정책이나 라이브러리는 추가하지 않는다.

## 현재 상태

`RegisterTenantRequest.password`는 `@Size(min = 8, max = 100)`과 `@Pattern`을 사용해 대문자, 소문자, 숫자, 특수문자 `@$!%*?&` 포함을 요구한다.

## 접근

- Bean Validation `Validator`를 직접 사용해 DTO validation 테스트를 추가한다.
- 강한 비밀번호는 위반이 없어야 한다.
- 대문자, 소문자, 숫자, 특수문자가 빠진 비밀번호는 각각 위반이 있어야 한다.
- `docs/improvement-todo.md`의 P2 #22를 완료로 갱신한다.

## 성공 기준

- 비밀번호 복잡도 조건이 자동 테스트로 검증된다.
- production code diff가 없다.
- `mvn test -pl user-service -am`과 `git diff --check`가 통과한다.
