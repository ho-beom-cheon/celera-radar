# P15-002 디자인/도움말 고도화

- 기준일: 2026-07-04
- 관련 이슈: #68
- 작업 브랜치: `codex/issue-68-p15-design-help-polish`
- 작업 범위: frontend UI source와 P15 문서 정리
- 제외 범위: backend, DB migration, 외부 API 연동, chart library 도입

## 1. 작업 목적

P15-001 기준선 분석에서 확인된 문제 중 다음 항목을 먼저 정리한다.

- 지표 설명이 화면마다 흩어져 있거나 없는 문제
- 로딩/빈 상태 표현의 일관성 부족
- 상품 검토 점수, 마진율, 가격 지표, 위험도 같은 판단 기준 설명 부족
- P15 이후 chart 도입 전 사용자가 현재 숫자의 의미를 이해할 수 있는 기본 help layer 필요

이번 작업은 차트 도입이 아니다. Recharts 등 chart library 도입은 `P15-003`에서 별도 판단한다.

## 2. 추가한 공통 구조

### Help content dictionary

- 파일: `frontend/src/lib/helpContent.ts`
- 역할: 화면에서 반복 사용하는 지표 설명을 key 기반으로 관리한다.
- 주요 key:
  - `candidateCount`
  - `searchResultCount`
  - `minPrice`
  - `avgPrice`
  - `maxPrice`
  - `competitionLevel`
  - `totalCost`
  - `expectedMargin`
  - `marginRate`
  - `supplyPrice`
  - `shippingFee`
  - `salePrice`
  - `recommendedSalePrice`
  - `targetMarginRate`
  - `productScore`
  - `riskLevel`
  - `lastSyncedAt`
  - `uploadFileFormat`
  - `requiredColumns`
  - `uploadEncoding`
  - `errorRows`
  - `alertRule`
  - `dataBaseDate`

### Help UI components

- `frontend/src/components/ui/HelpTooltip.tsx`
  - 지표 옆 `?` 버튼과 hover/focus tooltip을 제공한다.
- `frontend/src/components/ui/HelpText.tsx`
  - 상세 설명이 필요한 영역에 문장형 도움말을 표시한다.
- `frontend/src/components/ui/HelpPopover.tsx`
  - 이후 설정/가이드 영역에서 카드형 도움말로 재사용할 수 있게 추가했다.
- `frontend/src/components/ui/MetricCard.tsx`
  - `helpKey` prop을 추가해 summary/box 지표 카드에서 공통 tooltip을 연결한다.

## 3. 적용한 화면

| 화면 | 적용 내용 |
|---|---|
| 대시보드 | 후보 수 metric에 검토 후보 기준 도움말 추가 |
| 키워드 상세 | 스냅샷 기준일, 검색 결과 수, 최저/평균/최고가, 경쟁강도 도움말 추가 |
| 상품 후보 목록 | 현재 조건 후보 수, 점수, 예상 판매가, 공급가, 예상 마진율 도움말 추가 |
| 상품 후보 상세 | 상품 검토 점수 문장형 도움말 추가 |
| 도매 CSV/XLSX 업로드 | 파일 형식, 인코딩, 필수 컬럼, 배송비, 오류 행 도움말 추가 |
| 마진 계산기 | 공급가, 배송비, 목표 마진율, 판매가, 총 원가, 권장 판매가, 마진/마진율 도움말 추가 |
| 내 상품 마진 | 위험도 summary와 판매가/원가/마진/동기화 기준 도움말 추가 |
| 알림 조건 | 알림 조건 기준 도움말 추가 |

## 4. UI 보정

- `.metric-label`을 추가해 지표 라벨과 도움말 아이콘을 같은 줄에 안정적으로 배치했다.
- `.help-tooltip`을 hover/focus 모두에서 보이도록 구성했다.
- `.field-label-row`를 추가해 form label 내부에 interactive button이 중첩되지 않게 했다.
- `.table-heading-help`를 추가해 table header 안에서 도움말 아이콘이 열 너비를 과하게 흔들지 않도록 했다.
- `LoadingState`, `EmptyState`에 각각 `state-loading`, `state-empty` class를 추가해 상태 표현을 구분했다.

## 5. 유지한 제품 원칙

- 판매 보장, 수익 보장 표현은 추가하지 않았다.
- 모든 설명은 `검토`, `예상`, `데이터 기준`, `주의` 중심으로 작성했다.
- 네이버 API, 데이터랩 API, 스마트스토어 API 호출 경로는 변경하지 않았다.
- 도매 업로드와 마진 계산 기능의 기존 입력/응답 구조는 변경하지 않았다.

## 6. 검증 결과

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
tooltip hover: 마진율 도움말 노출 확인
mobile viewport: 390 x 800
horizontal overflow: false
console note: favicon.ico 404만 확인, 앱 오류 아님
```

확인 결과:

- tooltip 아이콘이 라벨과 겹치지 않는다.
- tooltip은 hover/focus에서 열린다.
- 버튼/입력 필드가 기존 기능을 방해하지 않는다.
- 360px 모바일 폭에서 주요 텍스트가 컨테이너를 벗어나지 않는다.

## 7. 다음 작업 제안

다음 작업은 `P15-003 Chart/KPI Visualization`이 적절하다.

후보 범위:

- Recharts 도입 여부 결정
- 키워드 상세 가격/경쟁 요약 시각화
- 후보 점수 breakdown bar 또는 radar 대체 시각화
- 내 상품 마진 위험 summary 시각화
- chart 도입 시 `npm run build`와 브라우저 시각 검증 필수
