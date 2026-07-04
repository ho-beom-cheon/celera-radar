# P15-008 Dashboard summary UX

## 작업 목적

Dashboard 요약 영역은 미로그인 상태에서도 후보 수가 `0`처럼 보이고, API 오류도 일반 notice로 표시되어 사용자가 현재 상태와 다음 행동을 구분하기 어려웠다.

이번 작업은 새 API를 만들지 않고 기존 후보 목록 API를 사용해 dashboard summary의 loading, signed-out, error, empty 상태를 분리하는 것이 목적이다.

## 반영 내용

- Dashboard 후보 요약 기본값을 `0`에서 `-`로 변경
- 후보 요약 상태를 `loading`, `signed-out`, `ready`, `error`로 분리
- 미로그인 상태에서 `EmptyState`로 계정 연결 안내 표시
- API 오류 상태에서 `ErrorState`와 `formatApiError` 사용
- 후보가 없는 ready 상태에서 empty 안내 표시
- 후보 요약 help key 범위를 `추천 검토 후보`, `검토 후보`, `전체 후보`에 맞게 정리

## 변경 파일

```text
frontend/src/app/App.tsx
docs/beta/P15_DASHBOARD_SUMMARY_UX.md
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
http://127.0.0.1:5174/

미로그인 상태:
- title: 오늘의 분석 요약
- summary card values: -, -, -, -
- empty message: 계정 연결 후 후보 요약을 확인할 수 있습니다.
- error banner count: 0
- active nav: 대시보드

API 오류 상태:
- localStorage fake token 설정 후 reload
- error message: 후보 요약을 불러오지 못했습니다.
- empty count: 0
- loading count: 0
```

## 제외 범위

- dashboard API endpoint 신규 구현 없음
- backend business logic 변경 없음
- DB migration 변경 없음
- 외부 API 연동 변경 없음
- chart 추가 없음
- 디자인 대개편 없음

## 남은 주의사항

- 로그인 성공 후 Dashboard summary는 기존 후보 목록 API 결과를 기준으로 표시한다.
- dashboard 전용 summary endpoint가 필요해지면 P15 이후 별도 backend/API 작업으로 분리한다.
- dev server smoke 중 backend 미실행에 따른 API fetch 오류는 의도적으로 error state 확인에 사용했다.

## 다음 작업 후보

P15-009는 `Form validation UX`가 적절하다.

후보 범위:

- 주요 form의 validation message 위치 일관화
- disabled action reason 표시
- submit 중복 클릭 방지 상태 점검
- required field 안내 정리
