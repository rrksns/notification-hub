# 상업용 백업·복구 설계

## 목적과 기준

상업 운영을 위해 데이터 저장소 장애, 운영자 실수, 릴리스 중 데이터 손상에서 복구할 수 있는 최소 백업 체계를 정의한다. 기존 운영 목표인 RPO 24시간 이하와 RTO 60분 이하를 유지한다. 이번 문서는 승인된 설계이며, AWS 인프라를 생성하거나 `terraform apply`하지 않는다.

## 승인된 결정

- AWS S3와 각 AWS 관리형 데이터 서비스의 백업 기능을 함께 사용한다.
- Kafka의 과거 메시지 본문을 별도로 장기 보관하지 않는다.
- Kafka 유실 복구는 MySQL에 저장된 미발행 Outbox 이벤트를 재발행할 수 있음을 보장하는 것으로 충분하다.
- 복구는 원본 리소스를 덮어쓰지 않고 격리된 새 리소스에 수행한 뒤 검증과 승인 후 전환한다.

## 현재 상태와 근거

- `terraform/aws/`는 AWS 구성 코드만 있으며 실제 적용되지 않았다. 따라서 AWS 백업은 현재 구성·검증된 상태가 아니다.
- 현재 `scripts/backup/` 및 `scripts/restore.sh`는 Compose 로컬 환경 전용이다. MySQL, MongoDB, Redis 백업 및 Kafka 토픽 메타데이터를 다루지만 관리형 AWS 백업 자동화로 사용할 수 없다.
- 2026-09-30 Compose 격리 복구 리허설에서 백업 6초, 복원 17초를 측정했으며 fixture 데이터와 Kafka 토픽 메타데이터를 검증했다. 외부 저장소 및 프로덕션 복구 리허설 증거는 아직 없다.
- 알림과 delivery 결과 Outbox는 각각 MySQL에 저장되며 `PENDING` 이벤트를 dispatcher가 Kafka로 재발행한다. 따라서 MySQL 시점 복구 후 복구 시점에 미발행인 Outbox 행이 남아 있는 경우 재발행할 수 있다.
- notification-service는 `PUBLISHED` Outbox 행을 기본 90일 이후 삭제한다. 따라서 본 설계는 이미 발행 완료된 과거 Kafka 메시지의 복원이나 재생을 보장하지 않는다.
- Redis에는 quota·멱등성 상태가 있으므로, 재생성 가능한 값인지와 snapshot 복원 후 보정이 필요한지 복구 리허설에서 확인한다.

## 백업 경계

| 데이터 | 1차 보호 | S3 사용 | 복구 보장 |
|---|---|---|---|
| MySQL (notification·delivery 및 두 Outbox 포함) | RDS 자동 백업과 point-in-time restore | 복구 매니페스트, 검증 증거 및 요구 시 논리 내보내기 | RPO 24시간 이하, 미발행 Outbox 재발행 |
| MongoDB 분석 데이터 | DocumentDB 지속 백업과 point-in-time restore | 복구 매니페스트 및 검증 증거 | RPO 24시간 이하 |
| Redis quota·멱등성 | ElastiCache 자동 snapshot | 복구 매니페스트 및 검증 증거 | 복구 후 핵심 key 및 TTL 검증 |
| Kafka | 토픽 설정/토픽 목록의 재생성 정보와 MySQL Outbox 재발행 | 토픽 메타데이터·복구 기록 보관 | 미발행 이벤트 재발행. 과거 Kafka 본문은 제외 |
| 애플리케이션 설정 | Terraform 및 버전 관리된 설정, 민감값은 Secrets Manager | 백업에 비밀값 복제 금지 | 인프라 재생성 및 Secret 참조 복구 |

AWS 관리형 백업이 서비스 소유의 S3 저장소를 사용하더라도, 그 파일은 고객 계정의 S3 객체로 직접 노출되지 않을 수 있다. 따라서 “관리형 백업이 존재함”과 “고객 소유 S3에 내보내기가 있음”을 별도 상태로 모니터링한다. 고객 S3는 관리형 backup 파일을 대신하는 저장소가 아니다.

## S3 보안과 보존

- 백업 전용 버킷을 Terraform state 버킷과 분리하고, 퍼블릭 액세스를 모두 차단한다.
- 기본 서버 측 암호화, 버전 관리, TLS 전송 강제, 최소 권한 IAM을 적용한다. KMS 고객 관리형 키 사용은 키 회전·복구 권한 및 비용을 포함해 적용 전 검토한다.
- 객체 경로는 환경·서비스·UTC 날짜·백업 식별자로 구분한다. 매니페스트에는 원본, snapshot 식별자, 시각, checksum, 앱/스키마 버전, RPO/RTO 검증 결과를 기록하고 인증정보나 개인정보를 넣지 않는다.
- AWS 관리형 RDS·DocumentDB 자동 백업/PITR과 ElastiCache 자동 snapshot의 보존기간은 15일로 확정한다. Terraform 변수와 운영 검증은 향후 구현 시 모두 15일을 사용한다.
- S3 export/증거물의 보존기간과 Object Lock 사용 여부는 고객 계약·비용 정책과 연결되므로 현재 확정하지 않는다. 구현 전 만료 규칙과 삭제 책임자를 결정해야 한다.

RDS와 DocumentDB는 1–35일 범위의 자동 백업/PITR을 제공하며, 선택한 15일 값은 해당 범위 안에 있다. RDS 자동 백업은 지정한 보존기간 중 시점 복구가 가능하고, DocumentDB는 연속 백업을 AWS 관리형 S3에 보관하며 해당 백업 파일은 고객이 직접 접근할 수 없다. ElastiCache의 Redis snapshot은 별도 retention/window를 구성해야 하며, 보존기간은 15일로 설정한다. 세부 설정은 AWS 공식 문서를 따른다.

## 실행 및 복구 흐름

1. 관리형 서비스 자동 백업을 매일 설정하고 백업 창을 UTC 기준으로 지정한다. 배포·스키마 변경 전에는 수동 snapshot을 추가한다.
2. 백업 성공 시각, 마지막 복구 가능 시각, S3 manifest 업로드 및 checksum을 확인하고 24시간 초과 시 운영 경보를 발생시킨다.
3. 장애 발생 시 영향 범위를 정하고 기존 리소스를 보존한다. 선택한 시점으로 별도 RDS/DocumentDB/ElastiCache 리소스를 복원하고 Kafka 토픽 설정을 재구성한다.
4. DB 연결, schema/migration 상태, 기준 fixture, pending Outbox 수와 재발행, Redis quota/멱등성 key, 서비스 health를 검증한다.
5. 담당자 승인 후 서비스 endpoint/secret 참조를 전환한다. 기존 데이터 리소스는 검증과 보존정책 확인 전 삭제하지 않는다.
6. 월 1회 격리 환경에서 전체 복구 리허설을 수행하고 RPO/RTO, 비용, 결과를 매니페스트와 운영 기록에 남긴다.

## 장애·위험 통제

- RDS·DocumentDB PITR은 새로운 리소스로 복원한 후 데이터 검증을 거친다. 원본 덮어쓰기나 즉시 파괴는 금지한다.
- MySQL 복구 시점이 Kafka보다 과거이면 Outbox가 이미 Kafka에 발행되었더라도 DB에는 pending으로 남아 중복 발행될 수 있다. consumer의 기존 멱등성 동작과 중복 영향도를 복구 리허설로 확인한다.
- Redis 복구 시 quota가 오래된 값으로 되돌아가 과다 사용 또는 사용 거절이 발생할 수 있다. snapshot 시점과 DB 기반 재계산 가능 여부를 검증한다.
- 백업 생성 성공은 복구 가능성을 증명하지 않으므로 정기 restore rehearsal을 완료하지 못하면 복구 보장을 주장하지 않는다.
- AWS 계정·리전·KMS 키 장애를 고려한 교차 리전 또는 계정 복사 정책은 비용 및 상업 요구사항이 확인되기 전까지 구현 범위에 포함하지 않는다.

## 승인 게이트

실제 AWS 생성 전 계정/리전, 데이터 보존기간, 예산 한도, KMS 키 관리 책임, 운영 알림 수신자를 확정한다. 이후 Terraform plan 검토, 비용 검토 및 비운영 환경 적용을 통과해야 한다. 현재는 문서와 실행 계획만 승인된 상태다.

## 공식 참고자료

- [Amazon RDS 자동 백업 보존기간 및 PITR](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_WorkingWithAutomatedBackups.BackupRetention.html)
- [Amazon DocumentDB 백업 및 복원](https://docs.aws.amazon.com/documentdb/latest/devguide/backup_restore.html)
- [Amazon S3 서버 측 암호화](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingEncryption.html)
- [Terraform AWS ElastiCache replication group 설정](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/elasticache_replication_group)
