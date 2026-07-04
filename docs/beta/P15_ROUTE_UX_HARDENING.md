# P15-006 Route UX Hardening

## 작업 목적

P15-001 기준선 분석에서 확인된 frontend routing 문제를 좁은 범위로 정리한다.

현재 앱은 React Router 없이 `window.location.pathname` 기반 수동 라우팅을 사용한다. 이번 작업은 새 라우터 의존성을 도입하지 않고, 현재 구조 안에서 unknown route, sidebar active state, 내부 링크 이동 UX를 먼저 보강하는 것이 목적이다.

## 반영 내용

- `App`의 route path를 React state로 관리하도록 변경
- 내부 링크 클릭을 History API 기반 SPA navigation으로 처리
- browser back/forward 이동을 `popstate`로 반영
- trailing slash path를 normalize
- unknown route에서 Not Found 안내 표시
- `/alert-rules`가 sidebar의 `알림` 항목을 active로 표시하도록 정리
- `/wholesale` legacy route가 sidebar의 `도매 업로드` 항목을 active로 표시하도록 정리
- active nav에 `aria-current="page"` 추가

## 변경 파일

```text
frontend/src/app/App.tsx
frontend/src/styles.css
docs/beta/P15_ROUTE_UX_HARDENING.md
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
http://127.0.0.1:5174/unknown-route
- title: 페이지를 찾을 수 없습니다
- active nav: null
- error banner count: 0

http://127.0.0.1:5174/alert-rules
- title: 알림 조건 설정
- active nav: 알림
- aria-current: 알림

internal nav click
- from: /alert-rules
- click: 키워드 레이더
- to: /keywords
- marker retained: kept
- active nav: 키워드 레이더
```

## 제외 범위

- React Router 도입 없음
- API endpoint 변경 없음
- backend business logic 변경 없음
- DB migration 변경 없음
- 외부 API 연동 변경 없음
- 디자인 대개편 없음

## 남은 주의사항

- 이번 작업은 수동 라우팅을 개선한 단계다. nested route, route loader, protected route 같은 구조가 필요해지면 React Router 도입을 별도 작업으로 검토한다.
- dev server console의 `favicon.ico` 404는 이번 변경과 무관한 기존 정적 자산 요청이다.

## 다음 작업 후보

P15-007은 `DataTable empty/loading structure`가 적절하다.

후보 범위:

- table 내부 loading/empty row 구조 일관화
- 주요 list 화면의 table overflow 점검
- empty row의 `colSpan` 기준 정리
- DataTable 공통 caption/aria 보강 검토
