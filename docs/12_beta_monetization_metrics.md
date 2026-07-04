# 12. Beta Readiness and Monetization Metrics

- 기준 작업: P14-001
- 기준 문서: `docs/05_refined_roadmap.md`, `docs/07_codex_execution_plan_v2.md`, `docs/10_database_design.md`
- 범위: 베타 운영 준비, 수익화 검증 지표, 요금제 가설
- 제외: 실제 결제 연동, 요금제 강제 적용 로직, backend/API/DB migration 구현, Apps in Toss Lite 구현

---

## 1. 원칙

셀러레이더의 수익화 검증은 "판매 성공"이나 "수익 보장"이 아니라 사용자가 데이터 기반 검토 후보를 반복적으로 확인하고 실제 운영 판단에 활용하는지를 검증한다.

사용 가능한 표현:

- 데이터 기반 후보
- 검토 후보
- 추천 검토 후보
- 마진 위험 감시
- 운영 판단 보조

피해야 할 표현:

- 매출 보장
- 수익 보장
- 무조건 팔리는 상품
- 자동 성공 상품

---

## 2. 베타 준비 체크리스트

| 영역 | 준비 항목 | 완료 기준 |
|---|---|---|
| 계정 | 이메일 가입/로그인, JWT refresh | 베타 사용자가 재로그인 없이 핵심 화면을 이용할 수 있다. |
| 키워드 | 키워드 등록, 분석 상태, 쇼핑 snapshot | 키워드 등록 후 분석 대기/성공/실패 상태를 구분할 수 있다. |
| 후보 | 도매 CSV 업로드, 후보 생성, 후보 저장/제외 | 사용자가 후보를 검토하고 저장/제외 의사결정을 남길 수 있다. |
| 마진 | 마진 계산, 스마트스토어 상품 원가 매핑, 마진 위험 대시보드 | 원가 미설정/주의/위험 상태를 구분할 수 있다. |
| 알림 | 알림 rule, 알림 생성 batch, 읽음 처리 | 조건 기반 알림이 생성되고 사용자가 확인 상태를 남길 수 있다. |
| 운영 | batch history, API call log, 사용량 지표 | 실패 원인과 사용량 흐름을 운영자가 추적할 수 있다. |
| 보안 | secret 미커밋, 사용자 데이터 격리, 업로드 파일 비공개 | 실제 사용자 데이터와 credentials가 노출되지 않는다. |
| 문구 | 보장 표현 제거 | 화면/문서에 매출 또는 수익 보장 표현이 없다. |

---

## 3. 핵심 지표

### 3.1 Activation

| 지표 | 설명 | 이벤트 후보 |
|---|---|---|
| signup_completed | 가입 완료 사용자 수 | `auth.signup.completed` |
| first_keyword_created | 첫 키워드 등록 완료 | `keyword.created` |
| first_snapshot_viewed | 첫 분석 결과 확인 | `shopping_snapshot.viewed` |
| first_csv_uploaded | 첫 도매 파일 업로드 | `wholesale_upload.previewed` |
| first_candidate_saved | 첫 후보 저장 | `candidate.saved` |
| first_store_product_cost_mapped | 첫 스마트스토어 상품 원가 매핑 | `store_product_cost.upserted` |

### 3.2 Engagement

| 지표 | 설명 |
|---|---|
| weekly_active_users | 최근 7일 내 핵심 이벤트가 있는 사용자 수 |
| keyword_create_count | 키워드 등록 수 |
| snapshot_view_count | 쇼핑 snapshot 조회 수 |
| candidate_review_count | 후보 상세 또는 점수 breakdown 조회 수 |
| candidate_saved_count | 저장된 후보 수 |
| alert_read_count | 읽음 처리된 알림 수 |
| store_margin_view_count | 내 상품 마진 대시보드 조회 수 |

### 3.3 Monetization Signal

| 지표 | 설명 |
|---|---|
| limit_hit_count | 요금제 제한 도달 횟수 |
| upgrade_cta_view_count | 업그레이드 안내 노출 수 |
| upgrade_cta_click_count | 업그레이드 안내 클릭 수 |
| paid_feature_attempt_count | 유료 후보 기능 시도 수 |
| beta_plan_interest_count | 베타 요금제 관심 표시 수 |

### 3.4 Retention

| 지표 | 설명 |
|---|---|
| d1_retention | 가입 다음 날 핵심 이벤트가 있는 사용자 비율 |
| d7_retention | 가입 7일 후 핵심 이벤트가 있는 사용자 비율 |
| d30_retention | 가입 30일 후 핵심 이벤트가 있는 사용자 비율 |
| weekly_repeat_usage | 2주 연속 핵심 이벤트가 있는 사용자 수 |

### 3.5 Data Quality

| 지표 | 설명 |
|---|---|
| naver_api_success_rate | 외부 API 호출 성공률 |
| snapshot_cache_hit_rate | 기존 snapshot 재사용률 |
| batch_success_rate | batch job 성공률 |
| csv_parse_success_rate | CSV/XLSX row 파싱 성공률 |
| cost_mapping_coverage | 스마트스토어 상품 중 원가 매핑 완료 비율 |

---

## 4. 요금제 가설

| Plan | 가설 | 기능 제한 후보 |
|---|---|---|
| FREE | 핵심 흐름 체험용 | 키워드 3개, CSV preview 제한, 알림 rule 제한, 스마트스토어 연결 제한 |
| BASIC | 1인 셀러 운영용 | 키워드 30개, CSV 업로드/후보 생성, 알림, 원가 매핑 |
| PRO | 반복 운영/확장용 | 키워드 100개 이상, 더 큰 CSV row, 리포트, 우선 batch 후보 |
| BETA | 초대 사용자 검증용 | 일정 기간 BASIC 또는 PRO 수준 기능을 제공하되 실제 결제는 연동하지 않음 |

요금제 문구는 가설로 유지한다. 실제 가격, 결제, 환불, 세금계산서, 구독 상태 전환은 별도 결제 연동 task에서 확정한다.

---

## 5. 기능 제한 기준

| 제한 항목 | FREE 후보 | BASIC 후보 | PRO 후보 | 측정 이벤트 |
|---|---:|---:|---:|---|
| active keywords | 3 | 30 | 100+ | `keyword.limit_hit` |
| CSV upload rows | preview 중심 | 5,000 | 50,000+ | `wholesale_upload.limit_hit` |
| alert rules | 1 | 10 | 50+ | `alert_rule.limit_hit` |
| smartstore connections | 0~1 | 1 | 3+ | `smartstore_connection.limit_hit` |
| store product cost mappings | 제한 | 기본 제공 | 확장 제공 | `store_product_cost.limit_hit` |
| manual refresh | 제한 | 일 단위 | 더 짧은 주기 후보 | `snapshot_refresh.limit_hit` |

제한 기준은 결제 전환 압박보다 실제 사용 패턴을 확인하기 위한 실험값으로 둔다.

---

## 6. 이벤트 설계

P14 사용량 지표는 `docs/10_database_design.md`의 `feature_usage_events`, `usage_metrics_daily` 초안을 기준으로 한다.

### 6.1 `feature_usage_events`

사용자 행동을 append-only 이벤트로 저장한다.

주요 필드 후보:

- `user_id`
- `occurred_at`
- `feature_key`
- `event_name`
- `plan_code`
- `entity_type`
- `entity_id`
- `metadata JSONB`

예시 이벤트:

```text
auth.signup.completed
keyword.created
shopping_snapshot.viewed
wholesale_upload.previewed
candidate.saved
candidate.excluded
alert.read
smartstore_product.synced
store_product_cost.upserted
store_margin_dashboard.viewed
plan.limit_hit
plan.upgrade_cta_clicked
```

### 6.2 `usage_metrics_daily`

운영/수익화 판단에 필요한 일별 집계값을 저장한다.

주요 필드 후보:

- `metric_date`
- `user_id`
- `plan_code`
- `active_keyword_count`
- `keyword_created_count`
- `snapshot_view_count`
- `csv_upload_count`
- `candidate_saved_count`
- `alert_read_count`
- `store_margin_view_count`
- `limit_hit_count`

---

## 7. 다음 구현 분리

P14-001 이후 구현은 아래처럼 분리한다.

1. `feature_usage_events`/`usage_metrics_daily` migration 및 repository
2. backend 이벤트 기록 port/service
3. 핵심 API 흐름별 usage event 기록
4. 운영자용 지표 조회 API
5. 업그레이드 안내 UI
6. 실제 결제 연동

이번 문서 작업에서는 위 항목을 구현하지 않는다.
