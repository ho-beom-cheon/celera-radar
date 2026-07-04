# P15-009 Form validation UX

## 작업 목적

주요 입력 폼에서 버튼이 비활성화된 이유와 필드별 검증 오류가 분리되어 보이지 않아 사용자가 다음 행동을 판단하기 어려웠다.

이번 작업은 신규 API나 폼 라이브러리를 도입하지 않고, 기존 React state 기반 폼에 필드 근처 메시지와 제출 상태를 일관되게 붙이는 것을 목적으로 한다.

## 반영 내용

- 공통 `FieldMessage` UI 컴포넌트 추가
- 필드 hint/error 메시지, 액션 비활성화 사유, category fieldset 오류 스타일 추가
- 키워드 등록 폼
  - 2~50자 입력 안내 표시
  - 빈 값/1자 입력 시 등록 버튼 하단 사유 표시
  - 1자 입력 시 필드 오류와 `aria-invalid` 표시
  - 제출 payload는 trim된 keyword 사용
- 도매 CSV/XLSX 업로드 폼
  - 파일 미선택, 지원하지 않는 확장자 메시지 표시
  - preview 전 컬럼 매핑 저장 불가 사유 표시
  - 상품명/공급가 필수 컬럼 메시지 표시
  - 파일 처리/저장 중 버튼 label 상태 표시
- 알림 조건 설정 폼
  - 조건명 필수 검증 표시
  - 최소 점수 0~100 범위 검증 표시
  - 최소 예상 마진율 0 이상 검증 표시
  - 카테고리 최소 1개 선택 검증 표시
  - 숫자 입력은 문자열 상태로 받은 뒤 제출 시 숫자로 변환
  - 저장 중 중복 제출 방지

## 변경 파일

```text
frontend/src/components/ui/FieldMessage.tsx
frontend/src/components/ui/index.ts
frontend/src/routes/keywords/KeywordsPage.tsx
frontend/src/routes/wholesale/WholesaleUploadPage.tsx
frontend/src/routes/alerts/AlertsPage.tsx
frontend/src/styles.css
docs/beta/P15_FORM_VALIDATION_UX.md
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
- 빈 키워드 상태에서 "키워드를 입력하세요." 표시 확인
- 1자 입력 시 "키워드는 2자 이상 입력하세요." 필드 오류와 버튼 하단 사유 확인

http://127.0.0.1:5174/wholesale/uploads
- 파일 미선택 상태에서 "CSV 또는 XLSX 파일을 선택하세요." 표시 확인
- preview 전 저장 확정 사유 "파일 preview 후 필수 컬럼을 선택하세요." 확인

http://127.0.0.1:5174/alert-rules
- 초기 유효 상태에서 "조건 저장" 버튼 활성 확인
- 최소 점수 120 입력 시 "최소 점수는 0~100 사이여야 합니다." 필드 오류와 버튼 하단 사유 확인

Playwright console:
- React DevTools 안내 외 error 0개
```

## 제외 범위

- backend API 변경 없음
- DB migration 변경 없음
- 외부 API 연동 변경 없음
- React Hook Form/Zod 도입 없음
- 전체 폼 체계 재작성 없음

## 남은 주의사항

- 후보 필터, 마진 계산기 같은 계산/필터 폼은 아직 같은 `FieldMessage` 패턴을 전면 적용하지 않았다.
- React Hook Form/Zod는 업로드/알림/API 설정 폼이 더 복잡해지는 시점에 별도 이슈로 검토한다.
- 현재 검증은 frontend UX 레벨이며 backend validation contract는 기존 DTO 기준을 따랐다.

## 다음 작업 후보

P15-010은 `Candidate filter validation UX` 또는 `Form validation pattern expansion`이 적절하다.

후보 범위:

- 후보 필터의 숫자 범위 검증 정리
- 마진 계산기 입력 검증 메시지 정리
- 반복되는 validation helper를 필요 범위 안에서 공통화
