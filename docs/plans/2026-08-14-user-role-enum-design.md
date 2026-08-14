# User Role Enum 설계

## 목표

`User.create()`에 직접 박혀 있는 `"ADMIN"` 문자열을 도메인 역할 enum으로 대체한다.

## 범위

- `User`의 `role` 필드 타입은 `String`으로 유지한다.
- JPA entity, mapper, JWT claim, token provider는 변경하지 않는다.
- 새 역할 정책이나 권한 기능은 추가하지 않는다.

## 접근

- `user-service` 도메인 모델 패키지에 `UserRole` enum을 추가한다.
- `User.create()`는 `UserRole.ADMIN.name()`을 사용해 기존과 같은 `"ADMIN"` 값을 만든다.
- 도메인 테스트는 신규 사용자 role이 `UserRole.ADMIN.name()`과 일치하는지 확인한다.

## 성공 기준

- `User.create()`에 역할 문자열 리터럴이 남지 않는다.
- 기존 저장값과 JWT claim에 쓰이는 role 값은 `"ADMIN"`으로 유지된다.
- `mvn test -pl user-service -am`과 `git diff --check`가 통과한다.
