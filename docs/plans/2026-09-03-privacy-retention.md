# 개인정보 보존/삭제 구현 계획

1. 개인정보 보존 기간과 삭제 범위를 문서화한다.
2. notification repository에 cutoff 기반 bulk delete 포트를 추가한다.
3. UTC 기준 scheduled retention service를 연결한다.
4. 삭제 기준 테스트와 전체 Maven 테스트를 실행한다.
5. 문서 상태 갱신 후 커밋, push, PR 병합을 진행한다.
