# DB 인덱스 재점검 설계

## 목표

현재 JPA repository 조회 조건과 Flyway 스키마 인덱스를 대조해 운영 경로에 필요한 누락 인덱스를 보강한다.

## 대상

- user-service `users.email`: 로그인은 tenant 조건 없이 이메일로 사용자를 조회한다.
- notification-service `notifications.created_at`: retention job은 cutoff 이전 생성일을 bulk delete한다.

tenant 조회 인덱스와 delivery/api key 조회 인덱스는 이미 존재하므로 변경하지 않는다.

## 검증 기준

- JPA entity와 Flyway migration에 같은 인덱스가 선언된다.
- 기존 repository 동작과 전체 테스트가 유지된다.
