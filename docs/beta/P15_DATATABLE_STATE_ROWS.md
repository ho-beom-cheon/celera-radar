# P15-007 DataTable empty/loading structure

## 작업 목적

P15 주요 목록 화면의 loading/empty 상태가 화면마다 다르게 표시되고 있었다.

일부 화면은 table 밖에 상태 메시지를 렌더링하고, 일부 화면은 `<tbody>` 안에 직접 `<tr><td colSpan>`을 작성했다. 이번 작업은 공통 `DataTableStateRow`를 추가해 table 상태 표시 구조를 맞추는 것이 목적이다.

## 반영 내용

- `DataTableStateRow` 공통 컴포넌트 추가
- `DataTableStateRow`를 공통 UI barrel export에 추가
- table state row 스타일 추가
- 알림 목록 table의 loading/empty 상태를 `<tbody>` 내부 row로 이동
- 키워드 목록 table의 loading/empty 상태를 `<tbody>` 내부 row로 이동
- 후보 목록 table의 loading/empty 상태를 `<tbody>` 내부 row로 이동
- 도매 CSV parsing 결과 table의 loading/empty 상태를 `<tbody>` 내부 row로 이동
- 내 상품 마진 table의 직접 `<tr><td colSpan>` 상태 row를 공통 컴포넌트로 교체

## 변경 파일

```text
frontend/src/components/ui/DataTable.tsx
frontend/src/components/ui/index.ts
frontend/src/routes/alerts/AlertsPage.tsx
frontend/src/routes/keywords/KeywordsPage.tsx
frontend/src/routes/candidates/CandidatesPage.tsx
frontend/src/routes/wholesale/WholesalePage.tsx
frontend/src/routes/store/StoreMarginsPage.tsx
frontend/src/styles.css
docs/beta/P15_DATATABLE_STATE_ROWS.md
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
http://127.0.0.1:5174/keywords
- tbody state rows: 1
- empty message: 계정 연결 후 키워드를 등록할 수 있습니다.
- outside state rows: 0

http://127.0.0.1:5174/candidates
- tbody state rows: 1
- empty message: 키워드 레이더에서 계정 연결 후 후보를 확인할 수 있습니다.
- outside state rows: 0

http://127.0.0.1:5174/store/margins
- tbody state rows: 1
- empty message: 계정 연결 후 내 상품 마진 상태를 확인할 수 있습니다.
- outside state rows: 0
```

## 제외 범위

- TanStack Table 도입 없음
- pagination/sorting/filtering 신규 구현 없음
- API endpoint 변경 없음
- backend business logic 변경 없음
- DB migration 변경 없음
- 디자인 대개편 없음

## 남은 주의사항

- `WholesaleUploadPage`의 preview table과 failure table은 preview/failure 결과 자체가 조건부 section 성격이라 이번 일괄 정리 대상에서 제외했다.
- 다음 단계에서 DataTable caption, column description, keyboard navigation까지 확장할 수 있다.

## 다음 작업 후보

P15-008은 `Dashboard summary UX` 또는 `Form validation UX`가 적절하다.

후보 범위:

- dashboard의 미로그인/empty 상태 안내 정리
- form validation message 위치 일관화
- disabled action reason 표시
- 주요 form submit 중복 클릭 방지 상태 점검
