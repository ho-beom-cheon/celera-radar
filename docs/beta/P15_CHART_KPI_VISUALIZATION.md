# P15-003 Chart/KPI Visualization

- 기준일: 2026-07-04
- 관련 이슈: #70
- 작업 브랜치: `codex/issue-70-p15-chart-kpi-visualization`
- 기준 브랜치: `codex/issue-68-p15-design-help-polish`
- 작업 범위: frontend chart dependency, chart UI component, 주요 KPI 화면 시각화
- 제외 범위: backend, DB migration, 외부 API 키 적용, 실제 네이버/스마트스토어 API 연동

## 1. 작업 목적

P15-002에서 지표의 의미를 도움말로 설명했다. P15-003에서는 사용자가 가격, 마진, 후보 점수, 위험 분포를 더 빠르게 비교할 수 있도록 chart/KPI 시각화를 추가한다.

이번 단계의 차트는 저장된 데이터와 기존 API 응답만 사용한다. 외부 API를 새로 호출하지 않으며 API key도 필요하지 않다.

## 2. 의존성 결정

도입 의존성:

```text
recharts ^3.9.2
```

선택 이유:

- React 18 peer dependency와 호환된다.
- 막대 차트와 도넛 차트만으로 이번 범위를 처리할 수 있다.
- 현재 자체 CSS/token 구조를 유지하면서 필요한 차트만 추가할 수 있다.

주의:

- Recharts 도입 후 frontend build는 성공했지만, Vite/Rolldown에서 minified chunk가 500kB를 넘는다는 경고가 발생한다.
- 이번 PR에서는 기능 범위를 넘기지 않기 위해 code splitting을 별도 적용하지 않았다.
- 다음 frontend 성능 정리 단계에서 route-level lazy loading 또는 chart component lazy loading을 검토한다.

## 3. 추가한 공통 UI

### KpiBarChart

- 파일: `frontend/src/components/ui/KpiBarChart.tsx`
- 역할: 가격, 마진, 후보 점수 같은 비교형 KPI를 막대 차트로 표시한다.
- 주요 props:
  - `title`
  - `description`
  - `data`
  - `helpKey`
  - `valueFormatter`
  - `maxDomain`
- 차트 하단에 값 목록을 함께 표시해 tooltip 없이도 숫자를 확인할 수 있게 했다.

### KpiDonutChart

- 파일: `frontend/src/components/ui/KpiDonutChart.tsx`
- 역할: 마진 위험 상태처럼 비율/분포를 보는 KPI를 도넛 차트로 표시한다.
- 현재 적용 대상:
  - 위험
  - 주의
  - 안전
  - 원가 미설정

## 4. 적용 화면

| 화면 | 적용 내용 |
|---|---|
| `/margin` | 총 원가, 권장 판매가, 입력 판매가, 입력 마진 비교 차트 |
| `/candidates/{id}` | 트렌드, 경쟁, 마진, 가격대, 공급, 위험 차감 점수 구성 차트 |
| `/store/margins` | 위험/주의/안전/원가 미설정 도넛 차트 |
| `/keywords/{id}` | 최저가, 평균가, 최고가 가격대 비교 차트 |

## 5. 제품 표현 기준

- `판매 가능성`, `수익 보장`, `잘 팔림 보장` 표현은 추가하지 않았다.
- 차트 설명은 모두 `검토`, `예상`, `비교`, `저장된 스냅샷` 기준으로 작성했다.
- 네이버 데이터랩 수치는 실제 판매량이 아니므로 이번 차트 범위에 추가하지 않았다.

## 6. API 키 준비 원칙

이번 P15-003은 API 키가 필요하지 않다.

API 키가 필요한 실제 외부 연동 단계로 들어가기 전에는 아래 내용을 먼저 정리한 뒤 진행한다.

| 연동 | 필요 값 | 적용 위치 |
|---|---|---|
| 네이버 쇼핑 검색 | `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` | 로컬 `.env`, 배포 환경변수, backend runtime env |
| 네이버 데이터랩 | `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `NAVER_DATALAB_DAILY_QUOTA` | 로컬 `.env`, 배포 환경변수, backend runtime env |
| 스마트스토어 Commerce API | client id/secret, redirect URI, token 관련 환경변수 | backend 환경변수와 스마트스토어 설정 문서 |

반영하지 않는 위치:

- frontend source
- Git tracked 문서의 실제 secret 값
- 테스트 fixture
- 로그 출력

## 7. 검증 결과

Frontend build:

```text
cd frontend && npm.cmd run build
BUILD SUCCESSFUL
```

Backend test:

```text
cd backend && .\gradlew.bat test
BUILD SUCCESSFUL
```

Browser smoke:

```text
검증 URL: http://127.0.0.1:5174/margin
chartPanels: 1
svgCount: 1
mobile viewport: 390 x 800
horizontal overflow: false

검증 URL: http://127.0.0.1:5174/store/margins
chartPanels: 1
마진 위험 분포 empty state 확인
horizontal overflow: false
```

주의:

```text
Vite/Rolldown chunk size warning 발생
dist/assets/index-*.js 약 590kB minified
```

처리 판단:

- build 실패가 아니므로 이번 PR에서는 기록만 남긴다.
- 다음 성능 정리에서 chart lazy loading을 검토한다.

## 8. 다음 작업 제안

다음 작업 후보는 `P15-004 API/Error UX Hardening` 또는 `P15-004 Chart Lazy Loading`이다.

추천 순서:

1. PR #69, PR #70 순서로 review/merge
2. chart chunk warning을 줄이기 위한 lazy loading 검토
3. 실제 API 연동 전 API key 발급/적용 가이드 작성
