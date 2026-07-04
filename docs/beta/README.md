# P15 beta work index

P15는 Web SaaS beta 전 화면 품질, 상태 처리, 차트, validation UX를 고도화한 작업 묶음이다.

이 디렉터리는 P15 진행 중 생성한 산출물 문서를 순서대로 보관한다.

## 문서 순서

| 단계 | 문서 | 목적 |
| --- | --- | --- |
| P15-001 | [P15_PROJECT_BASELINE_ANALYSIS.md](P15_PROJECT_BASELINE_ANALYSIS.md) | 프로젝트 기준선, 화면/문서/검증 상태 분석 |
| P15-002 | [P15_DESIGN_HELP_POLISH.md](P15_DESIGN_HELP_POLISH.md) | 디자인 토큰, 도움말, 기본 UI polish |
| P15-003 | [P15_CHART_KPI_VISUALIZATION.md](P15_CHART_KPI_VISUALIZATION.md) | Recharts 기반 KPI/차트 시각화 |
| P15-004 | [P15_CHART_LAZY_LOADING.md](P15_CHART_LAZY_LOADING.md) | chart chunk lazy loading 및 build warning 완화 |
| P15-005 | [P15_API_ERROR_UX_HARDENING.md](P15_API_ERROR_UX_HARDENING.md) | API 오류, 미로그인, loading/empty UX 정리 |
| P15-006 | [P15_ROUTE_UX_HARDENING.md](P15_ROUTE_UX_HARDENING.md) | SPA route fallback과 navigation hardening |
| P15-007 | [P15_DATATABLE_STATE_ROWS.md](P15_DATATABLE_STATE_ROWS.md) | DataTable loading/empty/error row 구조 정리 |
| P15-008 | [P15_DASHBOARD_SUMMARY_UX.md](P15_DASHBOARD_SUMMARY_UX.md) | dashboard summary 상태와 핵심 지표 UX 정리 |
| P15-009 | [P15_FORM_VALIDATION_UX.md](P15_FORM_VALIDATION_UX.md) | keyword/wholesale/alert form validation UX |
| P15-010 | [P15_CANDIDATE_FILTER_VALIDATION_UX.md](P15_CANDIDATE_FILTER_VALIDATION_UX.md) | candidate filter validation UX |
| P15-011 | [P15_MARGIN_CALCULATOR_VALIDATION_UX.md](P15_MARGIN_CALCULATOR_VALIDATION_UX.md) | margin calculator validation UX |
| P15-012 | [P15_VALIDATION_PATTERN_CLEANUP.md](P15_VALIDATION_PATTERN_CLEANUP.md) | validation helper 중복 정리 |
| P15-013 | [P15_CLOSEOUT_REVIEW.md](P15_CLOSEOUT_REVIEW.md) | P15 closeout review와 다음 단계 전 점검 |

## 현재 기준

- P15는 MVP 금지 범위인 AI 호출, SmartStore 자동 등록, 도매 direct API 연동을 포함하지 않는다.
- 외부 API key가 필요한 작업은 아직 진행하지 않았다.
- Apps in Toss Lite는 이후 단계로 보류한다.
- P15는 main에 반영된 상태다.
- 과거 stacked PR은 main 포함 여부 확인 후 superseded로 정리했다.

## 다음 연결

P15 closeout 이후에는 다음 중 하나를 별도 이슈/브랜치로 분리한다.

- P16 또는 beta readiness 후속 작업
- 실제 외부 API 연동 전 credential 준비 문서
- 사용자 학습용 프로젝트 회고/구현 설명 문서
