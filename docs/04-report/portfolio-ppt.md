# 포트폴리오 PPT 제작 기록

> 작성일: 2026-06-05 · 상태: **최종 승인 완료** (디자인 리뉴얼 반영)
> 빌드 도구: 전용 스크립트 `docs/04-report/build_portfolio.py` (초안은 `ppt-generator` 스킬로 생성)

## 산출물

- 파일: `Notification_Hub_포트폴리오.pptx` (프로젝트 루트, 13장) — 코랄 톤 + 실제 다이어그램/차트 적용
- 빌드 스크립트: `docs/04-report/build_portfolio.py`
- 발표자: 이설 · rrksns@nate.com

## 진행 경과

1. **초안 생성** — `ppt-generator` 스킬로 13장 구성(흐름 Why→What→How→So What). 시각 자료는 `[시각 자료]` 텍스트 지시문 상태.
2. **사용자 피드백** — ① 디자인 개선(Claude Code 문서 사이트 색감 참고) ② 텍스트 지시문을 실제 도형/차트로 렌더링.
3. **전면 리뉴얼** — 제너릭 템플릿 폐기, 전용 빌드 스크립트로 코랄 톤 + 도형 다이어그램 + 네이티브 차트 재제작.
4. **검증** — 구조 검증 통과(아래 검증 메모). LibreOffice 미설치로 이미지 렌더는 미수행.
5. **최종 승인** — 사용자 확인 결과 만족, 미세조정 불필요로 종료.

## 결정 사항 (입력값)

| 항목 | 값 | 비고 |
|------|-----|------|
| 대상 청중 | 혼합 (기술 + 비기술) | 도입부는 쉽게, 본문은 기술 깊이 |
| 발표 시간 | 10분 | 공식 `max(5, min(30, round(분×0.8)))` → 8장 |
| 실제 장수 | 13장 | 표지·목차·결론·Q&A + 섹션 구분 4장 포함 |

## 슬라이드 구성 (흐름: Why → What → How → So What)

| # | 슬라이드 | 핵심 |
|---|----------|------|
| 1 | 표지 | Notification Hub — Event-Driven MSA |
| 2 | 목차 | |
| 3–4 | 프로젝트 개요 & 문제 정의 | 멀티테넌트 알림 위임 모델, 테넌트 격리 |
| 5–6 | 시스템 아키텍처 | 6개 MSA + Clean Architecture, ArchUnit 검증 |
| 7–9 | 핵심 플로우 / 신뢰성 | Kafka 파이프라인, 멱등성·Circuit Breaker·재시도/DLQ, 원자적 집계 |
| 10–11 | 운영 & 품질 | Prometheus/Grafana/Zipkin, K8s/HPA, CI/CD, 커버리지 83.5~94.7% |
| 12 | 성과 요약 & 배운 점 | 개선 방향(실채널 연동·Outbox·E2E) 포함 |
| 13 | Q & A | |

## 콘텐츠 원칙

- 과장 배제: 채널 발송이 스텁이라는 점, AWS Terraform이 설계 코드만이라는 점을 솔직히 반영.
- 각 본문 슬라이드에 `key_message`(1문장)·`bullets`(3~4개)·발표자 노트 포함.

## 디자인 리뉴얼 (2026-06-05)

기존 제너릭 스킬 템플릿(파란 상단바 + "[시각 자료]" 텍스트 박스)을 폐기하고, **Claude Code 문서 사이트 톤**의 전용 빌드 스크립트(`build_portfolio.py`)로 전면 재제작.

### 디자인 토큰

| 역할 | 색상 |
|------|------|
| 배경(아이보리) | `#FAF9F5` |
| 카드/패널 | `#F2EFE7` · 보더 `#E0DACC` |
| 시그니처 코랄 | `#D97757` (강조 `#C2613F`) |
| 다크 섹션 | `#201F1B` · 본문 `#33312B` · 보조 `#6E6A60` |

- 좌측 코랄 액센트 바 + 미니멀 헤더(키커 → 타이틀 → 핵심 메시지 밴드) 공통 레이아웃.
- 폰트: `Apple SD Gothic Neo`.

### 텍스트 → 실제 시각화 교체

| 슬라이드 | 교체된 실제 시각 자료 |
|----------|----------------------|
| S4 문제 정의 | "각자 구축 vs Hub 위임" 비교 다이어그램 (도형+커넥터) |
| S6 아키텍처 | 동심원 Clean Architecture + 6개 MSA 토폴로지 |
| S8 파이프라인 | Client→Gateway→…→Analytics 가로 파이프라인 (Kafka는 실린더 도형) |
| S9 신뢰성 | 재시도→DLQ 흐름 + Circuit Breaker 상태 머신 |
| S11 운영·품질 | **네이티브 막대 차트** (커버리지 user 90.0 / notification 83.5 / delivery 93.4 / analytics 94.7) |

## 재현 방법

`python-pptx` 미설치 + Homebrew Python의 externally-managed 제약으로 venv 사용.

```bash
python3 -m venv /tmp/pptx-venv
/tmp/pptx-venv/bin/pip install -q python-pptx
/tmp/pptx-venv/bin/python docs/04-report/build_portfolio.py "Notification_Hub_포트폴리오.pptx"
```

## 검증 메모

- 구조 검증: 13장 정상 생성, S11 네이티브 차트 1개, S4/S6/S9 커넥터, 캔버스(13.33×7.5) 이탈 도형 0개.
- 시각 검증: 로컬에 LibreOffice 미설치로 이미지 렌더 미수행 → 좌표 산식·도형 인벤토리로 검증. PowerPoint/Keynote로 직접 열어 최종 확인 권장.

## 후속 작업 (TODO)

- [x] ~~PowerPoint에서 직접 열어 도형 정렬·줄바꿈 미세 조정~~ → 사용자 확인 결과 미세조정 불필요로 종료 (2026-06-05)
- [ ] 강조 기술 포인트 추가 검토: ArchUnit, Outbox 패턴
- [ ] 발표 리허설 후 슬라이드별 시간 배분 점검

> 문구·수치 변경이 필요하면 `build_portfolio.py`만 수정 후 재빌드(재현 방법 참고).
