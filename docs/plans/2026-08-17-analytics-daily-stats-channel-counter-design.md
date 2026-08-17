# Analytics DailyStats Channel Counter 설계

## 목표

`DailyStats` 도메인 모델 내부의 `Map<String, long[]>`를 제거한다.
성공/실패 카운트의 의미가 index 0/1에 묶이지 않도록 기존 `ChannelStats` record를 내부 카운터 값으로 사용한다.

## 접근

- `DailyStats` 내부 필드를 `Map<String, ChannelStats>`로 변경한다.
- `recordSuccess()`와 `recordFailure()`는 기존 카운터를 읽어 새 `ChannelStats` 값으로 교체한다.
- `DailyStats.reconstruct()`는 `Map<String, ChannelStats>`를 받는다.
- `DailyStatsDocument`는 기존 Mongo 저장 구조인 `Map<String, long[]>`를 유지하고, document/domain 변환 경계에서만 배열을 변환한다.

## 제외 범위

- MongoDB 저장 필드 구조는 변경하지 않는다.
- 원자적 `$inc` 경로는 기존 `channelCounts.{channel}.0/1` 업데이트를 유지한다.
- API 응답 DTO 구조는 이미 `ChannelStats`를 반환하므로 변경하지 않는다.

## 검증

- `DailyStatsTest`에 reconstruct가 `ChannelStats` 기반 카운터를 복원하는 테스트를 추가한다.
- focused analytics domain test, analytics module test, 전체 `mvn test`를 실행한다.
- `docs/improvement-todo.md`의 P2 #18 상태를 완료로 갱신한다.
