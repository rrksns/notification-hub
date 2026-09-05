# DB 인덱스 재점검 구현 계획

1. repository 조회 조건과 현재 인덱스를 대조한다.
2. users email과 notifications created_at 인덱스를 entity와 migration에 추가한다.
3. 전체 Maven 테스트와 migration 정합성을 검증한다.
4. 문서와 원격 브랜치를 갱신한다.
