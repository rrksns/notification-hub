# 이미지 배포 전략 계획

## 목표

`main`에 병합된 커밋을 재현 가능한 Docker 이미지로 게시하고, Kubernetes 운영 배포에서 immutable tag와 롤백 절차를 사용한다.

## 결정

- 레지스트리는 GitHub Container Registry(GHCR)를 사용한다.
- GitHub Actions `GITHUB_TOKEN`에 `packages: write` 권한을 부여한다.
- 각 서비스 이미지는 `${GITHUB_SHA}`와 `latest`로 게시한다.
- 운영 배포는 `${GITHUB_SHA}`만 사용하고, `latest`는 최신 성공 빌드 확인용으로만 둔다.
- 현재 로컬 Kubernetes 매니페스트의 `imagePullPolicy: Never`는 유지한다. 운영 클러스터에서는 GHCR pull Secret을 연결하고 `IfNotPresent`로 변경한다.
- 롤백은 Kubernetes Deployment의 이전 ReplicaSet으로 `kubectl rollout undo`를 사용한다.

## 구현 항목

1. CI build job에 GHCR 로그인과 이미지 push를 추가한다.
2. SHA 태그 배포, GHCR 인증, rollout 확인, 롤백 명령을 README에 기록한다.
3. 체크리스트와 context notes에 결정 및 검증 결과를 남긴다.

## 검증 기준

- GitHub Actions YAML에서 `packages: write`, GHCR login, `push: true`, SHA tag가 확인된다.
- 로컬 YAML 파싱과 diff 검사가 통과한다.
- 운영 배포 명령이 여섯 서비스의 이미지와 rollout을 동일한 SHA로 갱신한다.
- rollback 명령이 이전 ReplicaSet으로 복귀할 수 있다.
