# K8s Secret 평문 제거 설계

## 목표

운영 secret 값이 Git에 남지 않도록 `k8s/secret.yaml` 추적을 중단하고, Kubernetes 배포 전에 실제 Secret을 클러스터 안에서 생성하는 절차로 바꾼다.

## 접근 방식

선택한 방식은 `k8s/secret.example.yaml`만 Git에 보관하고 실제 `k8s/secret.yaml`은 `.gitignore`에 추가하는 것이다. 배포자는 `kubectl create secret generic notification-hub-secret --from-literal ... --dry-run=client -o yaml | kubectl apply -f -` 방식으로 Secret을 생성한다. Kubernetes 공식 `kubectl create secret generic` 명령은 literal, env file, client dry-run 출력을 지원하므로 별도 컨트롤러 없이 현재 plain YAML 배포 흐름과 맞는다.

## 대안

Sealed Secrets나 External Secrets Operator도 가능하지만, 컨트롤러 설치와 운영 백엔드 선택이 필요하다. 현재 목표는 Git에서 평문 secret을 제거하는 것이므로 의존성을 추가하지 않는 절차형 Secret 생성이 가장 작다.

## 변경 범위

- `k8s/secret.yaml`을 삭제하고 `.gitignore`에 추가한다.
- `k8s/secret.example.yaml`에 키 이름과 placeholder만 남긴다.
- MySQL, MongoDB K8s manifest의 literal password를 `secretKeyRef`로 바꾼다.
- README, PROCESS, manual test, checklist, context notes에 새 Secret 생성 절차를 기록한다.
- `JWT_SECRET`은 `JwtTokenProvider`가 Base64 decode해서 사용하므로 Base64 인코딩된 충분한 길이의 값을 넣도록 안내한다.

## 검증

- Git 추적 파일에 기존 secret 값이 남아 있지 않은지 검색한다.
- YAML 파서로 K8s manifest가 파싱되는지 확인한다.
- `kubectl create secret generic ... --dry-run=client -o yaml` 명령이 로컬에서 Secret YAML을 생성하는지 확인한다.
