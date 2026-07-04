# P15-011 Margin calculator validation UX

## 작업 목적

마진 계산기는 숫자 입력을 바로 number state로 저장하고 있어 입력칸을 비우면 즉시 `0`으로 바뀌고, 0 이하 공급가/판매가 또는 범위 밖 목표 마진율이 계산에 자동 보정될 수 있었다.

이번 작업은 P15-009에서 추가한 `FieldMessage` 패턴을 마진 계산기에 적용해, 필수 숫자 입력 오류와 권장가 적용 버튼 비활성화 사유를 명확히 표시하는 것을 목적으로 한다.

## 반영 내용

- 마진 계산기 입력 state를 number에서 string으로 변경
- 공급가 필수 입력 검증
  - 빈 값 오류
  - 숫자 아님 오류
  - 0 이하 오류
- 배송비 선택 입력 검증
  - 빈 값은 0으로 취급
  - 값이 있으면 0 이상 숫자 검증
- 목표 마진율 필수 입력 검증
  - 빈 값 오류
  - 숫자 아님 오류
  - 1~90 범위 검증
- 판매가 직접 입력 필수 검증
  - 빈 값 오류
  - 숫자 아님 오류
  - 0 이하 오류
- validation 오류가 있으면
  - 현재 마진율과 결과 metric을 `-`로 표시
  - KPI chart를 empty 상태로 표시
  - `권장가 적용` 버튼을 비활성화하고 사유 표시

## 변경 파일

```text
frontend/src/routes/margin/MarginCalculatorPage.tsx
docs/beta/P15_MARGIN_CALCULATOR_VALIDATION_UX.md
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
http://127.0.0.1:5174/margin

- 초기 유효 상태에서 현재 마진율 25.0%, 결과 metric, KPI chart 표시 확인
- 공급가 0 입력 시 "공급가는 0보다 커야 합니다." 필드 오류 확인
- 동일 오류가 권장가 적용 버튼 하단 사유로 표시되는지 확인
- invalid 상태에서 결과 metric이 `-`로 표시되는지 확인
- invalid 상태에서 KPI chart empty 상태 표시 확인
```

브라우저 검증 참고:

- console에는 `/favicon.ico` 404가 1건 표시됐다.
- 마진 계산기 기능 오류는 확인되지 않았다.

## 제외 범위

- backend API 변경 없음
- DB migration 변경 없음
- 마진 계산식 확장 없음
- 플랫폼 수수료/광고비/쿠폰비 입력 추가 없음
- 신규 validation 라이브러리 도입 없음

## 남은 주의사항

- 현재 Web 마진 계산기는 설계서의 전체 비용 항목 중 공급가, 배송비, 목표 마진율, 판매가만 다룬다.
- 플랫폼 수수료, 광고비, 쿠폰비 등 비용 항목 확장은 별도 화면/API 설계 이슈로 분리해야 한다.
- 앱인토스 Lite 빠른 마진 계산은 보류 상태이며 이번 작업에 포함하지 않았다.

## 다음 작업 후보

P15-012는 `Frontend validation pattern cleanup` 또는 `P15 closeout review`가 적절하다.

후보 범위:

- 반복되는 validation helper 중 실제 중복만 정리
- P15 산출물 문서 인덱스 정리
- 긴 PR stack merge 전 충돌/CI 상태 재점검
