# Release Gate 정적 자동화 설계

## 목표

상용 배포 전에 반복 가능한 저장소 검증을 하나의 fail-closed 명령으로 실행한다. 실제 Kubernetes CNI enforcement, rollout, smoke test, rollback, 별도 환경 restore는 운영 클러스터에서만 가능한 증적으로 분리한다.

## 범위

- 전체 Maven 검증.
- backup/restore 스크립트 Bash 문법 및 backup dry-run 검증.
- Docker Compose 구성 검증.
- Kubernetes YAML 구조 검증.
- Prometheus alert rule 검증.
- GitHub Actions에서 Maven 이후 정적 release gate 실행.

## 실패 정책

필수 도구나 입력이 없거나 하나의 검증 명령이라도 실패하면 즉시 비정상 종료한다. `--skip-maven`은 CI에서 이미 수행한 Maven 결과를 재사용할 때만 사용한다.

## 운영 경계

정적 게이트가 성공해도 실제 클러스터에서의 NetworkPolicy 차단, Gateway smoke test, rollback, 외부 백업 저장소 복원, RPO/RTO 증적이 없으면 출시를 승인하지 않는다. 운영 검증은 `docs/operations/release-gate.md` 절차를 따른다.

## 완료 기준

- `scripts/release/release-gate.sh`가 로컬과 CI에서 동일한 정적 검증을 실행한다.
- CI main pipeline이 정적 release gate를 이미지 게시 전에 실행한다.
- 정상 입력은 성공하고 필수 파일 누락 또는 잘못된 YAML은 실패한다.
