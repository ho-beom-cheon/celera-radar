# P15 프로젝트 기준선 분석

- 기준일: 2026-07-04
- 관련 이슈: #65
- 기준 브랜치: `codex/issue-65-p15-baseline-analysis`
- 기준 상태: P0~P12 구현 완료, P13 Apps in Toss Lite 보류, P14 베타/수익화 문서 정리 완료
- 작업 범위: 분석 문서 작성만 수행. 애플리케이션 소스, DB migration, API 구현은 변경하지 않음.

## 1. 프로젝트 구조

### 프론트 구조

- 위치: `frontend`
- 프레임워크: React 18, TypeScript, Vite
- 엔트리:
  - `frontend/src/main.tsx`
  - `frontend/src/app/App.tsx`
- 화면 디렉터리:
  - `frontend/src/routes/keywords`
  - `frontend/src/routes/candidates`
  - `frontend/src/routes/wholesale`
  - `frontend/src/routes/margin`
  - `frontend/src/routes/store`
  - `frontend/src/routes/alerts`
- API 래퍼:
  - `frontend/src/api/httpClient.ts`
  - `frontend/src/api/auth.ts`
  - `frontend/src/api/keywords.ts`
  - `frontend/src/api/candidates.ts`
  - `frontend/src/api/wholesale.ts`
  - `frontend/src/api/smartstore.ts`
  - `frontend/src/api/alerts.ts`
- 공통 UI:
  - `frontend/src/components/ui/DataTable.tsx`
  - `frontend/src/components/ui/MetricCard.tsx`
  - `frontend/src/components/ui/ProductCard.tsx`
  - `frontend/src/components/ui/StateMessage.tsx`
  - `frontend/src/components/ui/StatusBadge.tsx`
- 스타일:
  - `frontend/src/styles.css`
  - `frontend/src/styles/tokens.css`

### 백엔드 구조

- 위치: `backend`
- 프레임워크: Java 21, Spring Boot 4.1.0, Gradle
- 엔트리:
  - `backend/src/main/java/com/sellerradar/SellerRadarApplication.java`
- 주요 패키지:
  - `auth`: 회원가입, 로그인, JWT, 보안 필터
  - `user`: 사용자 도메인
  - `keyword`: 키워드 CRUD, 분석 API 응답
  - `shopping`: 네이버 쇼핑 검색 client, 스냅샷, 경쟁강도 계산
  - `trend`: 네이버 DataLab client, 트렌드 스냅샷, 트렌드 점수
  - `wholesale`: CSV/XLSX 업로드, preview, confirm, parsing, 후보 생성
  - `candidate`: 상품 후보, 점수, 상세/목록 API
  - `scoring`: 상품 검토 점수 계산
  - `alert`: 알림 규칙, 알림 생성 배치
  - `batch`: 관리자 배치 실행/이력
  - `smartstore`: 스마트스토어 연결, 상품 동기화, 원가 매핑, 정산 skeleton
  - `common`: 공통 응답, 에러, 외부 API 로그, request context
- DB migration:
  - `backend/src/main/resources/db/migration/V001__create_users.sql`
  - `backend/src/main/resources/db/migration/V002__create_keywords.sql`
  - `backend/src/main/resources/db/migration/V003__create_shopping_snapshots.sql`
  - `backend/src/main/resources/db/migration/V004__create_trend_snapshots.sql`
  - `backend/src/main/resources/db/migration/V005__create_wholesale_tables.sql`
  - `backend/src/main/resources/db/migration/V006__add_wholesale_upload_image_mapping.sql`
  - `backend/src/main/resources/db/migration/V007__create_alert_tables.sql`
  - `backend/src/main/resources/db/migration/V008__add_auth_columns_to_users.sql`
  - `backend/src/main/resources/db/migration/V009__create_naver_store_tables.sql`
  - `backend/src/main/resources/db/migration/V010__create_naver_store_products.sql`
  - `backend/src/main/resources/db/migration/V011__create_store_product_costs.sql`
  - `backend/src/main/resources/db/migration/V012__create_naver_order_settlement_snapshots.sql`

### 주요 디렉터리

- `.github`: GitHub Actions, issue/PR workflow
- `.idea`: IntelliJ 프로젝트 설정과 로컬 개발 실행 설정
- `backend`: Spring Boot API 서버
- `frontend`: React/Vite Web SaaS 화면
- `docs`: 설계, 실행 계획, 진행 요약, 베타/수익화 문서
- `docs/beta`: P15 산출물 문서 디렉터리. 이번 작업에서 신규 생성
- `docx`: DB 설계 docx 보관
- `infra`: 인프라용 Docker Compose
- `references`: 참고 자료 보관
- 루트 `docker-compose.yml`: 로컬 PostgreSQL 실행

### 주요 화면

- `/`: 대시보드 요약
- `/keywords`: 키워드 레이더 목록/등록/필터
- `/keywords/{id}`: 키워드 상세, 쇼핑 분석 실행, 상품 카드
- `/candidates`: 상품 후보 목록과 필터
- `/candidates/{id}`: 상품 후보 상세
- `/wholesale/uploads`: CSV/XLSX preview/confirm 업로드
- `/wholesale`: 기존 도매 파일 업로드/파싱/후보 생성 흐름
- `/margin`: 마진 계산기
- `/store/margins`: 스마트스토어 내 상품 마진 위험 대시보드
- `/alerts`: 알림 목록
- `/alert-rules`: 알림 규칙 생성

### 주요 서비스 파일

- 백엔드 외부 API:
  - `NaverShoppingClient`
  - `NaverDataLabClient`
  - `MockSmartStoreClient`
  - `MockSmartStoreSettlementClient`
- 백엔드 분석/수집:
  - `ShoppingSearchSnapshotService`
  - `CompetitionAnalyzer`
  - `TrendSnapshotService`
  - `DailyTrendBatchService`
  - `ShoppingSearchBatchService`
  - `CandidateService`
  - `ScoringEngine`
  - `WholesaleCandidateGenerationService`
  - `AlertGenerateBatchService`
- 프론트 API:
  - `apiRequest` 기반 fetch wrapper
  - 도메인별 API 함수 파일

## 2. 실행/빌드/테스트 명령어

### 설치

```bash
cd frontend
npm install
```

현재 `frontend/package-lock.json`과 `node_modules`가 존재하므로 P15-001에서는 의존성 변경을 하지 않았다.

### 개발 서버

```bash
docker compose up -d db
cd backend
./gradlew bootRun
cd frontend
npm run dev
```

Windows PowerShell에서는 `npm`이 실행 정책에 막힐 수 있으므로 직접 실행 시 `npm.cmd`를 사용한다.

```powershell
cd frontend
npm.cmd run dev
```

IntelliJ에서는 다음 파일을 Current File로 실행하면 DB, backend, frontend를 순차 실행한다.

```text
backend/src/test/java/com/sellerradar/dev/RunLocalDev.java
```

### 빌드

```bash
cd frontend
npm run build
```

P15-001 확인 결과:

```text
cmd.exe /c npm.cmd run build
SUCCESS
```

### 테스트

```bash
cd backend
./gradlew test
```

P15-001 확인 결과:

```text
backend ./gradlew.bat test
BUILD SUCCESSFUL
```

프론트엔드 `package.json`에는 `test` script가 없다. 다음 명령은 `--if-present`로 실행했으며 실행할 script가 없어 출력 없이 종료됐다.

```text
cmd.exe /c npm.cmd run test --if-present
exit 0, no configured script
```

### 린트

프론트엔드 `package.json`에는 `lint` script가 없다. 다음 명령은 `--if-present`로 실행했으며 실행할 script가 없어 출력 없이 종료됐다.

```text
cmd.exe /c npm.cmd run lint --if-present
exit 0, no configured script
```

### 런타임/패키지 매니저 확인

```text
node: v24.18.0
npm: 11.16.0
pnpm: 11.7.0
yarn: not installed
package lock: frontend/package-lock.json
current package manager: npm 기준
```

`frontend/package.json`의 Node engine은 `^20.19.0 || >=22.12.0`이므로 현재 Node 24.18.0은 조건을 만족한다.

## 3. 현재 사용 중인 오픈소스

| 영역 | 현재 상태 |
|---|---|
| UI | 자체 React 컴포넌트, 전역 CSS, CSS token |
| 라우팅 | React Router 없음. `window.location.pathname` 기반 수동 라우팅 |
| API 상태관리 | TanStack Query 없음. `useEffect`, `useState`, fetch wrapper 기반 |
| 테이블 | TanStack Table 없음. 자체 `DataTable` wrapper와 직접 `<table>` 구성 |
| 차트 | Recharts 등 차트 라이브러리 없음 |
| 폼 검증 | React Hook Form/Zod 없음. 브라우저 기본 검증과 수동 상태 처리 |
| 아이콘 | lucide-react 등 아이콘 라이브러리 없음 |
| 기타 | Vite, TypeScript, React DOM |

## 4. 현재 API 구조

### 공통 API 호출

- 프론트는 `frontend/src/api/httpClient.ts`의 `apiRequest<T>`를 통해 backend만 호출한다.
- 기본 base URL은 `VITE_API_BASE_URL`이며 기본값은 `http://localhost:8080/api/v1`이다.
- 인증 토큰은 `localStorage`의 `seller-radar.access-token`에 저장하고 `Authorization: Bearer` header로 전달한다.
- API 응답은 `ApiEnvelope<T>` 형식으로 받는다.
- 실패 시 `ApiRequestError`를 던지며 status, code, field를 가진다.

### 네이버 관련

- 서버 전용 호출 구조다. 프론트에서 Naver API를 직접 호출하지 않는다.
- 네이버 쇼핑 검색:
  - `NaverShoppingClient`
  - `ShoppingSearchSnapshotService`
  - `POST /api/v1/keywords/{keywordId}/analyze/shopping`
  - `GET /api/v1/keywords/{keywordId}/shopping-snapshot/latest`
- 네이버 DataLab:
  - `NaverDataLabClient`
  - `TrendSnapshotService`
  - `DailyTrendBatchService`
  - `POST /api/v1/admin/batches/datalab/run`
- 인증정보:
  - `NAVER_CLIENT_ID`
  - `NAVER_CLIENT_SECRET`
  - `NAVER_DATALAB_DAILY_QUOTA`
- 실패 처리:
  - 429는 `EXTERNAL_API_RATE_LIMIT`
  - 그 외 외부 API 실패는 `EXTERNAL_API_UNAVAILABLE`
- 외부 API 호출 이력:
  - `api_call_logs`
  - `ApiCallLog`
  - `ApiQuotaService`

### 스마트스토어 관련

- 현재는 skeleton/mock 중심이다.
- 주요 파일:
  - `SmartStoreConnection`
  - `SmartStoreProduct`
  - `SmartStoreProductSyncHistory`
  - `SmartStoreTokenCipher`
  - `SmartStoreProductSyncService`
  - `MockSmartStoreClient`
  - `SmartStoreSettlementClient`
  - `MockSmartStoreSettlementClient`
- 주요 API:
  - `POST /api/v1/smartstore/connections/{connectionId}/products/sync`
  - `GET /api/v1/smartstore/products`
  - `PUT /api/v1/smartstore/products/{productId}/cost`
  - `GET /api/v1/smartstore/products/{productId}/cost`
- 토큰 컬럼:
  - `access_token_encrypted`
  - `refresh_token_encrypted`
  - `token_expires_at`
- 현재 한계:
  - 실제 OAuth/Commerce API 연동 완료 상태가 아니다.
  - token refresh, 권한 부족, redirect URI, seller/shop id 등은 P15/P16에서 별도 확인이 필요하다.

### 업로드 관련

- 주요 API:
  - `POST /api/v1/wholesale-files`
  - `GET /api/v1/wholesale-files/{fileId}`
  - `POST /api/v1/wholesale-files/{fileId}/column-mapping`
  - `POST /api/v1/wholesale-files/{fileId}/parse`
  - `GET /api/v1/wholesale-files/{fileId}/rows`
  - `POST /api/v1/wholesale-files/{fileId}/candidates`
  - `POST /api/v1/wholesale-uploads/preview`
  - `POST /api/v1/wholesale-uploads/{uploadId}/confirm`
- 프론트는 `FormData`로 CSV/XLSX 파일을 업로드한다.
- parser는 CSV와 XLSX를 모두 지원한다.

### 마진 분석 관련

- 프론트 로컬 계산:
  - `MarginCalculatorPage`
- 백엔드 계산/점수:
  - `MarginCalculator`
  - `CandidateScoreCalculator`
  - `ScoringEngine`
  - `CandidateService`
- 스마트스토어 상품 원가/마진 감시:
  - `StoreProductCost`
  - `StoreProductCostService`
  - `StoreMarginsPage`

### 알림/배치 관련

- 알림:
  - `GET /api/v1/alerts`
  - `POST /api/v1/alert-rules`
  - `PATCH /api/v1/alerts/{alertId}/read`
- 배치:
  - `POST /api/v1/admin/batches/datalab/run`
  - `POST /api/v1/admin/batches/shopping-search/run`
  - `GET /api/v1/admin/batches`
- 주요 서비스:
  - `AlertGenerateBatchService`
  - `AlertGenerationService`
  - `DailyTrendBatchService`
  - `ShoppingSearchBatchService`

## 5. 현재 문제점

### 디자인

- P12에서 디자인 토큰과 기본 UI 정리는 진행됐지만, 아직 화면별 레이아웃/카드/필터/테이블 밀도가 완전히 통일되어 있지는 않다.
- `DataTable`은 wrapper 수준이어서 정렬, 필터, 페이지네이션, column visibility 같은 제품형 테이블 기능은 없다.
- 차트 컴포넌트가 없어 트렌드/가격/마진/알림 추이를 시각적으로 판단하기 어렵다.
- 도움말/지표 설명이 공통 시스템으로 분리되어 있지 않다.

### UX

- 라우팅이 `window.location.pathname`과 `<a href>` 기반이라 SPA 전환, NotFound, ProtectedRoute, route 기반 active state 확장이 제한적이다.
- 각 페이지가 `useEffect`, `useState`, `loading`, `message`, `error` 상태를 반복 구현한다.
- 일부 화면은 empty/loading/error 상태가 있지만, 표현 수준과 위치가 화면마다 다르다.
- 사용자에게 보여줄 에러 메시지와 개발자용 에러 원인의 분리가 더 필요하다.
- PowerShell 출력 기준으로 일부 한글 문자열이 깨져 보이는 현상이 있어 실제 브라우저 표시와 파일 인코딩 확인이 필요하다.

### API

- 프론트 API layer는 단순 fetch wrapper 중심이다.
- 401/403/429/500에 대한 사용자 행동 안내가 중앙화되어 있지 않다.
- API cache, 재요청, mutation 후 invalidate, retry 정책이 없다.
- 스마트스토어와 정산/수수료는 skeleton/mock 범위가 남아 있어 실제 연동 완료로 표현하면 안 된다.
- API 인증정보가 필요한 외부 연동은 문서화와 사용자 요청 목록이 필요하다.

### 상태관리

- 서버 상태와 UI 상태가 페이지 컴포넌트 내부 state로 흩어져 있다.
- 목록/상세/동기화/업로드 후 재조회 정책이 수동 구현되어 있다.
- 로그인 토큰과 plan은 localStorage에 저장되지만, 토큰 만료/refresh 흐름의 프론트 UX는 더 다듬어야 한다.

### 예외처리

- 백엔드 공통 오류 포맷은 존재한다.
- 프론트는 `ApiRequestError`를 사용하지만, status/code별 사용자 안내와 복구 액션이 표준화되어 있지 않다.
- 업로드 실패 행/컬럼/원인 표시는 일부 존재하지만 P15 회귀 테스트에서 빈 파일, 잘못된 확장자, 필수 컬럼 누락, 숫자 오류, 음수 가격, 대용량 파일을 별도 점검해야 한다.

### 문서화

- `docs/11_progress_summary.md`가 최신 진행 상태를 가장 잘 반영한다.
- `docs/05_refined_roadmap.md`의 초반 phase 표는 일부 오래된 상태가 남아 있어 최신 진행 상태와 혼동될 수 있다.
- P15 산출물용 `docs/beta` 디렉터리는 이번 작업 전까지 없었다.

## 6. P15 변경 대상 후보

| 파일/영역 | 변경 이유 | 영향 범위 | 검증 방법 |
|---|---|---|---|
| `frontend/src/app/App.tsx` | 수동 라우팅, layout, navigation 정리 필요 | 전체 화면 진입과 sidebar active state | `npm run build`, 브라우저 route 확인 |
| `frontend/src/components/ui/*` | 카드, 테이블, 상태, 도움말 공통화 필요 | 모든 주요 화면 UI | `npm run build`, 화면별 시각 점검 |
| `frontend/src/styles.css` | 화면별 스타일 누적, 컴포넌트 스타일 통일 필요 | 전체 UI | `npm run build`, 반응형 확인 |
| `frontend/src/styles/tokens.css` | 디자인 token은 존재하나 chart/table/help 상태 확장 필요 | 전체 UI tone과 spacing | `npm run build`, CSS token scan |
| `frontend/src/api/httpClient.ts` | status/code별 에러 표준화, token 만료 UX 필요 | 모든 frontend API 호출 | `npm run build`, 401/403/429/500 케이스 점검 |
| `frontend/src/api/*.ts` | query/mutation hook 도입 후보 | 모든 server state 조회/변경 | `npm run build`, 기능별 API smoke test |
| `frontend/src/routes/**/*.tsx` | 반복된 loading/error/form/table 로직 정리 | 주요 화면 | `npm run build`, 페이지별 회귀 테스트 |
| `backend/src/main/java/com/sellerradar/common/external` | 외부 API 호출 이력/쿼터/에러 정책 정리 후보 | 네이버/스마트스토어/API 확장 | `./gradlew test` |
| `backend/src/main/java/com/sellerradar/shopping` | 쇼핑 API adapter와 normalizer 정리 후보 | P2/P3 분석 | `./gradlew test`, MockWebServer test |
| `backend/src/main/java/com/sellerradar/trend` | DataLab API adapter와 quota 정책 정리 후보 | P4 trend/batch | `./gradlew test`, MockWebServer test |
| `backend/src/main/java/com/sellerradar/smartstore` | 실제 API 전 인증정보 요청/실패 케이스 문서화 필요 | P9~P11 | `./gradlew test`, mock failure test |
| `docs/beta/*` | P15 산출물 작성 | 문서 | markdown diff, 민감정보 점검 |
| `.env.example` | P15 API 확장 시 필요한 sample env 보강 후보 | 로컬 설정 문서화 | secret 미포함 확인 |

## 7. 오픈소스 도입 가능성

| 라이브러리 | 도입 가능성 | 이유 | 리스크 |
|---|---|---|---|
| TanStack Query | 높음 | 현재 API 조회가 `useEffect + useState`로 반복되고 cache/invalidate/retry 정책이 없다. | 한 번에 모든 화면을 바꾸면 회귀 범위가 커진다. keyword/candidate/smartstore 조회부터 점진 적용 권장 |
| React Router | 높음 | 현재 수동 라우팅은 NotFound, ProtectedRoute, nested route, SPA 전환에 약하다. | `App.tsx`와 navigation이 전체 화면에 영향. P15-002 또는 P15-003에서 작은 route 단위로 적용 필요 |
| TanStack Table | 중간 | 테이블 화면이 많고 정렬/필터/페이지네이션 요구가 커질 가능성이 높다. | 현재 자체 table이 단순해서 도입 효과는 있지만 초기 column 정의 작업이 많다. 핵심 목록 1~2개부터 적용 권장 |
| Recharts | 중간 | 트렌드/가격/마진/알림 추이를 시각화할 데이터가 있다. | 차트 남발 위험. 판단에 필요한 KPI 중심으로 제한 적용 필요 |
| shadcn/ui | 낮음 | 현재 Tailwind 기반이 아니며 전역 CSS/token 구조를 사용한다. | Tailwind/Radix 전제 구조로 바꾸면 P15 범위가 커진다. 즉시 도입보다 개념만 참고 권장 |
| Radix UI | 중간 | tooltip/popover/dialog 같은 접근성 컴포넌트에 유용하다. | 새 의존성 도입 전 현재 요구는 HelpTooltip/Popover로 좁혀 검토 필요 |
| lucide-react | 높음 | 메뉴, 상태, 도움말, 업로드, 알림 아이콘에 적합하고 도입 비용이 낮다. | 아이콘 남발 및 의미 전달을 아이콘에만 의존하는 문제. 텍스트/aria-label 병행 필요 |
| React Hook Form | 중간 | 업로드, 알림 규칙, API 설정, 검색 필터 폼이 늘어날수록 유용하다. | 단순 폼까지 전면 교체하면 변경량이 커진다. 검증 복잡도가 높은 업로드/알림 설정부터 적용 권장 |
| Zod | 중간 | 숫자/날짜/파일/URL/API key 검증을 중앙화할 수 있다. | backend validation과 중복될 수 있다. 사용자 입력 UX 개선 목적의 frontend schema로 한정 권장 |

## 8. P15 우선순위 제안

1. `P15-002` 디자인/도움말 전 먼저 브라우저에서 한글 표시와 주요 화면 시각 상태를 확인한다.
2. HelpTooltip/HelpPopover와 `helpContent` 사전을 먼저 추가한다.
3. `App.tsx` layout/navigation을 정리하고 React Router 도입 여부를 확정한다.
4. API 조회 구조는 TanStack Query를 후보로 두되, 한 번에 모든 API를 바꾸지 않고 keyword/candidate/smartstore 중 반복이 큰 영역부터 적용한다.
5. DataTable은 현재 wrapper를 유지하면서 정렬/empty/loading 구조를 보강할지, TanStack Table로 갈지 P15-003에서 결정한다.
6. Recharts는 트렌드와 마진 위험 대시보드처럼 판단에 직접 필요한 화면부터 제한적으로 적용한다.
7. P15-004에서 실제 외부 API 확장은 인증정보 요청 문서를 먼저 만들고, 인증 없는 구현 완료 처리는 금지한다.

## 9. P15-001 검증 결과

| 명령 | 결과 | 비고 |
|---|---|---|
| `cmd.exe /c node -v` | 성공 | `v24.18.0` |
| `cmd.exe /c npm.cmd -v` | 성공 | `11.16.0` |
| `cmd.exe /c pnpm -v` | 성공 | `11.7.0`, 현재 프로젝트 기준은 npm |
| `cmd.exe /c yarn -v` | 실패 | yarn 미설치 |
| `cmd.exe /c npm.cmd run build` | 성공 | frontend build 성공 |
| `cmd.exe /c npm.cmd run test --if-present` | 성공 | test script 없음 |
| `cmd.exe /c npm.cmd run lint --if-present` | 성공 | lint script 없음 |
| `backend ./gradlew.bat test` | 성공 | backend test 성공 |

## 10. 다음 작업 추천

다음 작업은 `P15-002 디자인/도움말 고도화`가 적절하다.

단, 시작 전에 다음을 먼저 요약해야 한다.

- 수정할 frontend 파일 목록
- HelpTooltip/HelpPopover 구조
- 적용할 도움말 항목
- React Router/TanStack Query/TanStack Table/Recharts를 이번 단계에서 도입할지 여부
- build와 주요 화면 확인 방법
