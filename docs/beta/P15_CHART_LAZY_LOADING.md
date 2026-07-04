# P15-004 Chart Lazy Loading

## 작업 목적

P15-003에서 Recharts 기반 KPI 차트를 도입한 뒤 frontend build는 성공했지만, 초기 JavaScript chunk가 500kB를 넘는 Vite/Rolldown warning이 발생했다.

이번 작업은 차트 기능을 유지하면서 Recharts 관련 코드를 초기 번들에서 분리하는 것이 목적이다.

## 반영 내용

- `KpiBarChart`, `KpiDonutChart`의 props type을 export하도록 정리
- `LazyKpiBarChart`, `LazyKpiDonutChart` 래퍼 추가
- 공통 UI barrel export에서 Recharts를 직접 import하는 chart component export 제거
- 차트 사용 화면을 lazy wrapper 사용으로 변경
- chart loading fallback 스타일 추가

## 변경 파일

```text
frontend/src/components/ui/KpiBarChart.tsx
frontend/src/components/ui/KpiDonutChart.tsx
frontend/src/components/ui/LazyKpiCharts.tsx
frontend/src/components/ui/index.ts
frontend/src/routes/margin/MarginCalculatorPage.tsx
frontend/src/routes/keywords/KeywordDetailPage.tsx
frontend/src/routes/candidates/CandidateDetailPage.tsx
frontend/src/routes/store/StoreMarginsPage.tsx
frontend/src/styles.css
docs/beta/P15_CHART_LAZY_LOADING.md
docs/11_progress_summary.md
```

## Build 결과

```text
cd frontend
npm.cmd run build

BUILD SUCCESSFUL
```

주요 결과:

```text
dist/assets/KpiDonutChart-D1q7Vknh.js      18.45 kB
dist/assets/KpiBarChart-DAqoynWN.js        53.13 kB
dist/assets/index-D9iw9o9v.js             220.21 kB
dist/assets/CategoricalChart-asXnCBk7.js  302.80 kB
```

확인 결과:

- 이전 P15-003 build에서 발생한 500kB chunk size warning은 더 이상 발생하지 않는다.
- Recharts 관련 chart chunk가 초기 index chunk와 분리되었다.

## Smoke 확인

```text
http://127.0.0.1:5174/margin
```

확인 결과:

- 마진 계산기 화면 로드 성공
- lazy-loaded KPI bar chart 렌더링 성공
- SVG chart count: 1
- chart loading fallback 잔존 없음

주의:

- dev server console의 `favicon.ico` 404는 이번 변경과 무관한 기존 정적 자산 요청이다.

## Backend 확인

```text
cd backend
.\gradlew.bat test

BUILD SUCCESSFUL
```

## 제외 범위

- API endpoint 변경 없음
- DB migration 변경 없음
- 외부 API 연동 변경 없음
- API key 또는 secret 추가 없음
- 차트 지표 또는 디자인 대개편 없음

## 다음 작업 후보

P15-005는 `API/Error UX Hardening`이 적절하다.

후보 범위:

- API 오류 메시지 표시 정합성 점검
- 인증 만료/미로그인 상태 UX 정리
- empty/loading/error 상태 일관화
- 문서 기준과 화면 문구의 보장 표현 여부 재점검
