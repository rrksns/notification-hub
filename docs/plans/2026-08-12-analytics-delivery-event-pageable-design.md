# Analytics Delivery Event Pageable 설계

## 목표

`analytics-service`의 `DeliveryEventRepository.findByTenantId()`가 테넌트 전체 이벤트를 한 번에 조회하지 않도록 `Pageable` 기반 조회로 바꾼다.

## 범위

- 새 REST API는 만들지 않는다.
- 기존 미사용 repository port와 Mongo adapter의 조회 시그니처만 페이징 형태로 바꾼다.
- `PageResponse` DTO 변환이나 controller 변경은 이번 범위에서 제외한다.

## 접근

- domain port `DeliveryEventRepository`의 조회 메서드를 `Page<DeliveryEvent> findByTenantId(String tenantId, Pageable pageable)`로 변경한다.
- infrastructure Mongo repository는 `Page<DeliveryEventDocument> findByTenantId(String tenantId, Pageable pageable)`를 반환한다.
- adapter는 Spring Data `Page.map(...)`을 사용해 메타데이터를 보존한 채 document를 domain model로 변환한다.
- adapter 단위 테스트로 `Pageable` 전달과 `Page` 메타데이터 보존을 검증한다.

## 성공 기준

- 기존 전체 목록 반환 API가 사라진다.
- tenant 조회는 호출자가 반드시 `Pageable`을 전달해야 한다.
- `mvn test -pl analytics-service -am`과 `git diff --check`가 통과한다.
