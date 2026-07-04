# P15-012 Frontend validation pattern cleanup

## 목적

P15-009부터 P15-011까지 폼 validation UX를 확장하면서 여러 화면에 같은 형태의 validation helper가 반복되었다.

이번 작업은 새 기능을 추가하지 않고, 반복된 helper만 공통 유틸로 분리해 이후 P15/P16 화면 확장 시 같은 패턴을 재사용할 수 있게 하는 것을 목적으로 한다.

## 반영 범위

```text
frontend/src/lib/formValidation.ts
frontend/src/routes/alerts/AlertsPage.tsx
frontend/src/routes/candidates/CandidatesPage.tsx
frontend/src/routes/margin/MarginCalculatorPage.tsx
frontend/src/routes/wholesale/WholesaleUploadPage.tsx
docs/beta/P15_VALIDATION_PATTERN_CLEANUP.md
docs/11_progress_summary.md
```

## 변경 내용

- `hasFormErrors`를 공통 helper로 분리
- 문자열 입력의 blank 판정을 `isBlank`로 분리
- 문자열 숫자 입력의 finite number 파싱을 `parseFiniteNumber`로 분리
- 후보 필터의 빈 값 유지용 숫자 변환을 `blankToNumberFilter`로 분리
- alert rule, candidate filter, margin calculator, wholesale upload 화면의 local helper 중복 제거

## 유지한 정책

- validation 메시지와 표시 위치는 변경하지 않는다.
- alert rule의 최소 점수는 필수이며 0~100 범위를 유지한다.
- alert rule의 최소 예상 마진율은 필수이며 0 이상 범위를 유지한다.
- candidate filter의 최소 점수와 최소 마진율은 빈 값을 허용하고, 값이 있으면 기존 범위를 유지한다.
- margin calculator의 공급가, 목표 마진율, 판매가는 필수 숫자 입력 정책을 유지한다.
- margin calculator의 배송비는 빈 값을 0으로 취급하는 기존 정책을 유지한다.
- wholesale upload의 파일/컬럼 mapping validation 정책은 변경하지 않는다.

## 제외 범위

- 새 validation rule 추가
- API 요청/응답 변경
- 백엔드, DB, migration 변경
- 외부 API 연동 변경
- 디자인 대개편

## 검증 결과

```text
cd frontend && npm.cmd run build
BUILD SUCCESSFUL

cd backend && .\gradlew.bat test
BUILD SUCCESSFUL
```

이번 작업은 helper 추출 중심이라 브라우저 동작은 기존 P15-009~011 검증 결과를 기준으로 유지한다.

## 다음 작업

P15-013은 `P15 closeout review`가 적절하다.

검토 항목:

- P15 산출물 문서 인덱스 정리
- P15에서 변경한 화면의 회귀 확인
- stacked PR 병합 순서와 남은 CI 상태 확인
- P16 또는 다음 기능 단계로 넘어가기 전 보류/주의사항 정리
