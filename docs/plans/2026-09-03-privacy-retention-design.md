# 개인정보 보존/삭제 설계

## 목표

notification-service에 저장되는 수신자와 메시지 본문의 보존 기간을 제한해 불필요한 개인정보 잔존을 방지한다.

## 범위

- `notifications`의 `recipient`와 `content`를 생성 시점 기준 90일 후 하드 삭제한다.
- 삭제 작업은 매일 UTC 03시에 실행한다.
- 삭제 기준일과 삭제 건수는 운영 로그로 남긴다.
- 기존 알림 조회 API와 delivery/analytics 이력은 이번 범위에서 변경하지 않는다.
- 삭제 작업은 재실행 가능하도록 cutoff 기준의 멱등적인 bulk delete로 처리한다.

## 설정

| 설정 | 환경변수 | 기본값 |
|---|---|---|
| 보존 기간 | `NOTIFICATION_RETENTION_DAYS` | `90` |
| 실행 cron | `NOTIFICATION_RETENTION_CRON` | `0 0 3 * * *` |

## 검증 기준

- cutoff가 UTC 기준 현재 시각에서 보존 기간만큼 이전으로 계산된다.
- repository가 cutoff 이전 데이터만 삭제한다.
- 삭제 건수가 로그에 기록된다.
