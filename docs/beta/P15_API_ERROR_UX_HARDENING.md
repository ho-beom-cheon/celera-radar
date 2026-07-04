# P15-005 API/Error UX Hardening

## 작업 목적

P15 화면에서 API 오류, 미로그인, loading, empty 상태 처리가 화면별로 중복 구현되어 있었다.

이번 작업은 API나 DB를 변경하지 않고 frontend의 오류 메시지 변환과 인증 필요 상태 표시를 정리하는 것이 목적이다.

## 반영 내용

- `formatApiError` 공통 helper 추가
- `authRequiredMessage` 공통 helper 추가
- route별 중복 `errorMessage` 함수 제거
- API 공통 오류 포맷의 `message`, `field`를 사용자 화면에 일관되게 표시하도록 정리
- 알림 목록 미로그인 상태를 오류 배너가 아닌 empty 안내로 변경
- 내 상품 마진 미로그인 상태를 오류 배너가 아닌 empty 안내로 변경

## 변경 파일

```text
frontend/src/lib/apiError.ts
frontend/src/routes/alerts/AlertsPage.tsx
frontend/src/routes/keywords/KeywordsPage.tsx
frontend/src/routes/keywords/KeywordDetailPage.tsx
frontend/src/routes/candidates/CandidatesPage.tsx
frontend/src/routes/candidates/CandidateDetailPage.tsx
frontend/src/routes/store/StoreMarginsPage.tsx
frontend/src/routes/wholesale/WholesalePage.tsx
frontend/src/routes/wholesale/WholesaleUploadPage.tsx
docs/beta/P15_API_ERROR_UX_HARDENING.md
docs/11_progress_summary.md
```

## 검증 결과

```text
cd frontend
npm.cmd run build

BUILD SUCCESSFUL
```

```text
cd backend
.\gradlew.bat test

BUILD SUCCESSFUL
```

Browser smoke:

```text
http://127.0.0.1:5174/alerts
- error banner count: 0
- empty message: 계정 연결 후 알림을 확인할 수 있습니다.

http://127.0.0.1:5174/store/margins
- error banner count: 0
- empty message: 계정 연결 후 내 상품 마진 상태를 확인할 수 있습니다.
```

## 제외 범위

- API endpoint 변경 없음
- backend business logic 변경 없음
- DB migration 변경 없음
- 외부 API 연동 변경 없음
- API key 또는 secret 추가 없음
- 디자인 대개편 없음

## 남은 주의사항

- `App.tsx`의 API 서버 연결 상태 메시지는 health check 성격이 달라 이번 범위에서 유지했다.
- `ANALYSIS_NOT_READY`, `STORE_PRODUCT_COST_NOT_FOUND`처럼 code 기반 분기가 필요한 화면은 `ApiRequestError`를 계속 직접 참조한다.
- dev server console의 `favicon.ico` 404는 이번 변경과 무관한 기존 정적 자산 요청이다.

## 다음 작업 후보

P15-006은 `Route UX Hardening` 또는 `DataTable empty/loading structure`가 적절하다.

후보 범위:

- manual `window.location.pathname` 기반 routing 정리 검토
- route not found 처리
- DataTable 내부 loading/empty row 구조 일관화
- 주요 list 화면의 table overflow와 empty row 접근성 점검
