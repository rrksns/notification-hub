# 상업 운영 백업·복구 구현 계획

> **실행 안내:** 각 Task를 순서대로 진행하고, AWS 리소스 생성 전 승인 게이트에서 멈춘다. 이 문서는 구현 계획이며 현재 인프라를 적용하지 않는다.

**목표:** RPO 24시간 이하와 RTO 60분 이하를 목표로 AWS 관리형 데이터 저장소 백업, 별도 S3 증거/내보내기, 격리 복원과 운영 증적을 구축한다.

**구성:** RDS·DocumentDB point-in-time recovery 및 ElastiCache snapshot을 각 서비스의 관리형 기능으로 활성화한다. 전용 private S3 버킷에는 manifest, 검증 결과와 필요성이 승인된 portable export를 저장한다. Kafka는 역사적 본문을 아카이브하지 않고 MySQL pending Outbox 재발행으로 복구한다. 기존 Compose 스크립트는 로컬 rehearsal 전용으로 유지한다.

**기술:** Terraform AWS modules, AWS RDS/DocumentDB/ElastiCache, private S3, IAM/KMS, AWS CLI, 기존 Maven 검증 및 운영 문서.

---

## Task 1: 백업 정책 변수와 보안 요구사항 정의

**파일:** `terraform/aws/variables.tf`, `terraform/aws/modules/rds/`, `terraform/aws/modules/documentdb/`, `terraform/aws/modules/elasticache/`

1. 현재 provider 버전과 module 입력/호출 구조를 확인한다.
2. 환경별 백업 retention 및 UTC backup window를 명시적 Terraform 변수로 정의한다. 기본값은 무백업을 허용하지 않고 상업 운영 정책에 맞는 7일 이상을 제안하되, 최대치/비용 선택은 승인 게이트에서 확인한다.
3. RDS의 automated backup retention과 window, DocumentDB의 retention/window, ElastiCache의 snapshot retention/window에 변수를 연결한다.
4. Terraform validation 또는 정적 테스트로 범위와 필수값을 검증한다.

**완료 기준:** Terraform validate 및 계획 테스트가 통과하고, `0` 또는 서비스 제한 밖 보존값이 거부된다.

## Task 2: 전용 S3 백업 증거 버킷 구성

**파일:** `terraform/aws/modules/backup-storage/`, AWS root module 호출부

1. Terraform state와 분리된 환경별 백업 전용 S3 bucket을 만든다.
2. public access block, 버전 관리, 기본 암호화, TLS-only bucket policy를 구성한다. KMS customer-managed key는 비용·운영 책임 승인 후 적용한다.
3. manifest/증거물용 prefix와 writer/reader IAM 역할을 최소 권한으로 제한한다. 애플리케이션 실행 역할에 bucket-wide 삭제 권한을 주지 않는다.
4. 승인된 보존기간이 정해지기 전에는 자동 만료 및 Object Lock을 활성화하지 않는다.
5. 버킷 이름/ARN과 암호화 키 식별자를 비밀 없이 출력한다.

**완료 기준:** Terraform plan에서 public access가 열리지 않고, writer가 지정 prefix에만 쓰며 일반 workload가 삭제하지 못함을 검증한다.

## Task 3: 관리형 서비스 백업 구성 검증

**파일:** `terraform/aws/modules/rds/main.tf`, `terraform/aws/modules/documentdb/main.tf`, `terraform/aws/modules/elasticache/main.tf` 및 해당 테스트

1. 백업 retention/window를 실제 AWS provider schema에 맞춰 추가한다.
2. RDS deletion protection, final snapshot 동작을 유지하고 identifier와 태그로 환경을 식별한다.
3. DocumentDB PITR 설정과 ElastiCache snapshot retention을 명시적으로 검증한다.
4. static Terraform test 또는 validate로 환경별 설정이 누락되지 않았음을 확인한다.
5. `terraform plan`은 비운영 입력으로만 검토한다. 사용자 승인 없이 `terraform apply`하지 않는다.

**완료 기준:** RDS·DocumentDB·Redis의 보존정책이 plan에 나타나며 비운영 plan 외 실제 AWS 리소스 변화가 없다.

## Task 4: 매니페스트와 백업 상태 경보

**파일:** `scripts/backup/` 또는 AWS 운영 스크립트, `monitoring/`, IAM/Terraform 구성

1. 관리형 snapshot/PITR 식별자와 생성·복구 가능 시각을 조회하는 읽기 전용 검증 작업을 추가한다.
2. 각 백업 증거에 환경, 리소스 식별자, UTC 시각, checksum(내보내기 파일에 한함), schema/application 버전, 검증 결과를 기록한다.
3. 24시간보다 오래된 마지막 유효 복구 지점, snapshot 생성 실패, S3 업로드/checksum 실패를 경보한다.
4. Secret, 고객 메시지 본문, 불필요한 개인정보가 로그/manifest에 들어가지 않는지 확인한다.
5. 실패 시 0이 아닌 종료 코드와 actionable log를 내고, 성공으로 오인하지 않도록 한다.

**완료 기준:** 신선한 백업/오래된 백업/누락된 artifact 시나리오를 테스트하고, 경보가 실제로 구분된다.

## Task 5: 복구 Runbook과 격리 복구 자동화

**파일:** `docs/operations/backup-recovery.md`, `scripts/backup/` 복구 검증 도구, `manual_test.md`

1. 사고 범위 판별, 복원 시점 선택, 담당자 승인, 복구 리소스 생성, 앱 검증, endpoint 전환, 원본 보존 절차를 작성한다.
2. restore 작업은 기존 production 리소스를 덮어쓰지 않고 새 식별자 또는 격리 환경에 생성한다.
3. MySQL의 두 Outbox backlog와 재발행을 검증하고 중복 consumer 동작을 확인한다.
4. DocumentDB fixture/핵심 컬렉션, Redis quota/멱등성 key·TTL, Kafka 토픽 설정 및 서비스 health를 점검한다.
5. production 승인과 수동 전환 전에는 검증 환경에만 적용한다.

**완료 기준:** 데이터별 검증 결과와 RPO/RTO 측정값을 남기고 담당자가 복구 후 정상 기능을 확인한다.

## Task 6: 비용 검토 및 AWS 비운영 적용 게이트

1. 선택한 리전, retention, 데이터 크기, PITR 변경량, snapshot 저장량, S3/KMS/모니터링 비용을 추산한다.
2. 계정·리전·월 예산·보존기간·KMS 키 운영자·경보 수신자를 승인받는다.
3. 승인된 비운영 계정에만 Terraform plan/apply를 진행하고 예상 비용과 실제 비용을 기록한다.
4. 관리형 백업 복원과 S3 manifest 검증을 수행한다.
5. 비용 또는 RTO/RPO를 만족하지 못하면 상업 운영 적용을 중지하고 설계를 수정한다.

**완료 기준:** 비운영 AWS 복원 리허설이 RPO ≤24시간 및 RTO ≤60분을 만족하고, 담당자 승인과 비용 기록이 남는다.

## 검증 명령

```bash
terraform -chdir=terraform/aws fmt -check -recursive
terraform -chdir=terraform/aws validate
mvn test
```

실제 plan 검증에는 자격 증명이나 네트워크 대상이 필요할 수 있으므로 승인된 비운영 환경에서만 수행한다. 로컬 Compose restore 테스트는 로컬 스크립트 회귀 확인일 뿐 AWS PITR 검증으로 대체하지 않는다.

## 완료 후속 작업

- [ ] 운영자 승인된 AWS account/region 및 budget 확보.
- [ ] 실제 managed backup 정책과 S3 버킷을 비운영 계정에 적용.
- [ ] AWS 격리 restore rehearsal을 실행하고 RPO/RTO를 증명.
- [ ] 월간 rehearsal 일정과 알림 수신자 등록.
