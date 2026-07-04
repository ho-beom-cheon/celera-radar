# P15-010 Candidate filter validation UX

## 작업 목적

상품 후보 필터의 숫자 입력은 `minScore`, `minMarginRate` 값을 문자열로 받지만, 범위 밖 값이 들어와도 필드 근처 안내가 없고 자동 조회 흐름에서 잘못된 query가 만들어질 수 있었다.

이번 작업은 P15-009에서 추가한 `FieldMessage` 패턴을 후보 필터 폼에 좁게 확장해, 숫자 검증 오류와 조회 버튼 비활성화 사유를 일관되게 표시하는 것을 목적으로 한다.

## 반영 내용

- 후보 필터 폼에 `FieldMessage` 적용
- `minScore`는 빈 값 허용, 값이 있으면 0~100 범위 검증
- `minMarginRate`는 빈 값 허용, 값이 있으면 0 이상 검증
- 범위 오류가 있으면 후보 조회 API 호출 전에 중단
- 조회 버튼 하단에 비활성화 사유 표시
- 공백 입력은 빈 필터로 취급하도록 query payload 변환 기준을 `trim()`으로 정리
- 미로그인/로딩/검증 오류 상태의 조회 버튼 label과 disabled reason 정리

## 변경 파일

```text
frontend/src/routes/candidates/CandidatesPage.tsx
docs/beta/P15_CANDIDATE_FILTER_VALIDATION_UX.md
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
http://127.0.0.1:5174/candidates

- fake token 상태에서 후보 필터 초기 진입 확인
- 초기 유효 상태에서 "조회" 버튼 활성 확인
- 최소 점수 120 입력 시 "최소 점수는 0~100 사이여야 합니다." 필드 오류 확인
- 동일 오류가 조회 버튼 하단 사유로 표시되는지 확인
```

브라우저 검증 참고:

- fake token으로 후보 목록 초기 조회를 유도했기 때문에 backend 미연결/CORS fetch 오류가 console에 표시됐다.
- 검증 대상은 후보 필터 validation UI이며, backend/API 계약 변경은 없다.

## 제외 범위

- backend API 변경 없음
- DB migration 변경 없음
- 후보 조회 조건 의미 변경 없음
- 마진 계산기 입력 검증 변경 없음
- 신규 validation 라이브러리 도입 없음

## 남은 주의사항

- 후보 필터는 아직 자동 조회와 수동 submit 흐름이 함께 존재한다. 이번 작업에서는 invalid query를 차단하는 선에서만 정리했다.
- 필터 적용 방식 자체를 수동 submit 전용으로 바꾸는 작업은 별도 UX 정책 결정이 필요하다.
- 마진 계산기 입력 검증은 다음 후보로 남긴다.

## 다음 작업 후보

P15-011은 `Margin calculator validation UX`가 적절하다.

후보 범위:

- 마진 계산기 필수 숫자 입력 검증
- 0 이하 가격/원가 입력 안내
- 계산 버튼 비활성화 사유 표시
