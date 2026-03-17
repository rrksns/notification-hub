# Discovery Service 전체 플로우

## 역할

Netflix Eureka 기반의 **서비스 레지스트리**입니다. 모든 마이크로서비스가 시작 시 여기에 등록하고, 다른 서비스를 찾을 때 이 레지스트리를 조회합니다.

---

## 디렉토리 구조

```
discovery-service/
├── pom.xml
└── src/main/
    ├── java/com/notificationhub/discovery/
    │   └── DiscoveryServiceApplication.java
    └── resources/
        └── application.yml
```

---

## 동작 방식

```
[discovery-service 시작 - Port 8761]
        ↓
Eureka Server 활성화 (@EnableEurekaServer)
        ↓
각 마이크로서비스 시작 시 자기 자신을 등록
  ├─ api-gateway      (8080)
  ├─ user-service     (8081)
  ├─ notification-service (8082)
  ├─ delivery-service (8083)
  └─ analytics-service (8084)
        ↓
서비스 간 호출 시 Eureka에서 주소 조회
  예: api-gateway → lb://notification-service
      → Eureka에서 notification-service 인스턴스 목록 조회
      → 로드밸런싱 후 실제 IP:Port로 라우팅
```

---

## 주요 설정

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false   # 자기 자신은 등록하지 않음
    fetch-registry: false         # 자기 자신이 레지스트리이므로 조회 불필요
  server:
    enable-self-preservation: false        # 개발 환경: 자기 보호 모드 비활성화
    eviction-interval-timer-in-ms: 5000   # 5초마다 비정상 인스턴스 제거
```

**self-preservation 비활성화:** 네트워크 장애 시 죽은 서비스를 즉시 제거 (운영 환경에서는 활성화 권장)

---

## 기술 스택

| 항목 | 내용 |
|------|------|
| 서버 포트 | 8761 |
| 서비스 디스커버리 | Netflix Eureka Server |
| 모니터링 | Spring Boot Actuator (health, info) |
