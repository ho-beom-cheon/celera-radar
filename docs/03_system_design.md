# 셀러레이더 전체 설계서

- 문서 버전: v0.1
- 작성일: 2026-07-02
- 대상 단계: Web SaaS v0.1~v0.4, 앱인토스 Lite v1.0
- 서비스 가칭: 셀러레이더
- 문서 목적: 전체 아키텍처, 데이터 구조, 배치, 점수화, 운영/보안/로드맵을 정의한다.

---

## 1. 프로젝트 개요

셀러레이더는 초보 스마트스토어/위탁판매 셀러가 상품 후보를 찾을 때 사용하는 데이터 기반 검토 도구다. 네이버 쇼핑 검색 가격대, 네이버 데이터랩 쇼핑인사이트 검색 클릭 추이, 사용자가 업로드한 도매 CSV 데이터를 결합해 상품 후보를 점수화하고, 조건에 맞는 후보가 발견되면 알림을 제공한다.

핵심 포지션:

```text
실시간 상품 검색 서비스가 아니라, 매일 갱신되는 셀러용 상품 발굴 레이더.
```

---

## 2. 목표와 비목표

## 2.1 목표

1. 키워드별 네이버 쇼핑 가격대/경쟁강도 분석
2. 키워드별 검색 클릭 추이 기반 트렌드 점수 계산
3. 도매 CSV 기반 예상 마진 계산
4. 추천점수와 사유/주의점 제공
5. 조건 충족 시 앱 내부 알림 생성
6. Web SaaS 본체와 앱인토스 Lite를 하나의 백엔드로 운영

## 2.2 비목표

MVP에서는 다음을 하지 않는다.

1. AI 기반 상품 추천 자동 생성
2. 실제 판매량 보장 또는 판매 예측 보장
3. 스마트스토어 자동 상품 등록
4. 스마트스토어 주문/정산 API 연동
5. 도매 API 직접 연동
6. 외부 푸시/이메일 알림
7. 별도 네이티브 모바일 앱

---

## 3. 사용자와 플랜

## 3.1 초기 타깃

| 사용자 | 설명 | 우선순위 |
|---|---|---|
| 초보 스마트스토어 셀러 | 상품을 찾고 마진 계산이 필요한 사용자 | 1순위 |
| 위탁판매 셀러 | 도매 CSV를 가지고 상품 후보를 찾는 사용자 | 1순위 |
| 해외구매대행 셀러 | 환율/배송/통관 변수가 있는 사용자 | 후순위 |

## 3.2 요금제 초안

| 플랜 | 월 가격 후보 | 키워드 | CSV 행 | 알림 | 리포트 |
|---|---:|---:|---:|---|---|
| FREE | 0원 | 3개 | 100행 | 제한 | 없음 |
| BASIC | 2,900~4,900원 | 30개 | 3,000행 | 일 1회 | 기본 |
| PRO | 5,900~9,900원 | 100개 | 20,000행 | 조건별 | 상세 |

가격은 초기 검증 후 조정한다.

---

## 4. 전체 아키텍처

```text
[Web SaaS React]
        │
        ├──────────────┐
        │              │
[앱인토스 Lite React]  │
        │              │
        └───────>[Spring Boot API]
                       │
                       ├─ PostgreSQL
                       ├─ Scheduler
                       ├─ CSV Parser
                       ├─ Scoring Engine
                       ├─ Alert Engine
                       │
                       ├─ Naver Shopping Search API
                       ├─ Naver DataLab Shopping Insight API
                       ├─ Apps in Toss IAP - future
                       └─ Naver Commerce API - future
```

---

## 5. 기술 스택

| 영역 | 스택 | 선택 이유 |
|---|---|---|
| Frontend | React + TypeScript + Vite | Codex 구현 용이, Web SaaS와 앱인토스 WebView 재사용 |
| Backend | Spring Boot + Java 21 | 사용자의 SI/Spring 역량과 적합, 안정적 API 서버 |
| DB | PostgreSQL | 관계형 데이터/JSONB/snapshot 저장에 적합 |
| ORM | Spring Data JPA | 빠른 개발 |
| Query | QueryDSL은 v0.3 이후 선택 | 복잡 필터가 늘어난 뒤 도입 |
| Batch | Spring Scheduler | MVP 단순화 |
| CSV | Apache Commons CSV 또는 OpenCSV | 안정적 파싱 |
| Auth | Spring Security + JWT | Web SaaS MVP 인증 |
| Infra | Docker Compose | 로컬/소규모 배포 단순화 |
| CI/CD | GitHub Actions | PR 단위 검증 |
| AI | MVP 제외 | 토큰 최소화 |

---

## 6. 모듈 구조

```text
backend
├─ auth
├─ user
├─ plan
├─ keyword
├─ category
├─ shopping
├─ trend
├─ wholesale
├─ margin
├─ candidate
├─ scoring
├─ alert
├─ batch
├─ external
│  ├─ naver
│  └─ intoss
└─ common
```

프론트엔드 구조:

```text
frontend
├─ src
│  ├─ app
│  ├─ routes
│  │  ├─ dashboard
│  │  ├─ keywords
│  │  ├─ candidates
│  │  ├─ wholesale
│  │  ├─ margin
│  │  ├─ alerts
│  │  └─ toss-lite
│  ├─ components
│  ├─ api
│  ├─ stores
│  └─ utils
```

---

## 7. 핵심 데이터 흐름

## 7.1 키워드 분석 흐름

```text
사용자 키워드 등록
→ keyword_master 저장
→ 다음 배치 대상 선정
→ 네이버 쇼핑 검색 API 호출
→ shopping_price_snapshot 저장
→ 네이버 데이터랩 API 호출
→ trend_snapshot 저장
→ scoring engine 실행
→ keyword_analysis_summary 저장
→ 후보/알림 생성
```

## 7.2 도매 CSV 후보 생성 흐름

```text
CSV 업로드
→ 파일 메타 저장
→ 컬럼 매핑
→ row 파싱
→ wholesale_product 저장
→ 상품명 정규화
→ 키워드/쇼핑 가격대 매칭
→ 예상 판매가/마진 계산
→ candidate 저장
→ 추천점수 계산
```

## 7.3 알림 생성 흐름

```text
candidate score 생성
→ alert_rule 비교
→ 중복 알림 체크
→ alert 저장
→ Web SaaS/앱인토스 알림 목록 표시
```

---

## 8. DB 설계 초안

## 8.1 주요 테이블 목록

| 테이블 | 설명 |
|---|---|
| users | 사용자 |
| subscription_plan | 요금제 |
| user_subscription | 사용자 구독 상태 |
| category_master | 내부 카테고리 |
| risk_category_rule | 추천 제외/주의 카테고리 룰 |
| keyword_master | 사용자/관리자 키워드 |
| shopping_price_snapshot | 네이버 쇼핑 검색 스냅샷 |
| shopping_top_item | 쇼핑 검색 상위 상품 |
| trend_snapshot | 데이터랩 트렌드 스냅샷 |
| keyword_analysis_summary | 키워드별 최신 분석 요약 |
| wholesale_file | CSV 업로드 파일 |
| wholesale_product | CSV 파싱 상품 |
| product_candidate | 추천 후보 |
| candidate_score | 추천점수와 이유 |
| margin_calculation | 빠른 마진 계산 저장 |
| alert_rule | 알림 조건 |
| alert | 알림 |
| batch_job_history | 배치 실행 이력 |
| api_call_log | 외부 API 호출 로그 |

---

## 8.2 테이블 상세

### users

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 사용자 ID |
| email | varchar unique | 이메일 |
| password_hash | varchar | 해시 비밀번호 |
| role | varchar | USER/ADMIN |
| plan_code | varchar | FREE/BASIC/PRO |
| created_at | timestamp | 생성일 |
| updated_at | timestamp | 수정일 |

### keyword_master

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 키워드 ID |
| user_id | bigint nullable | 사용자 키워드, null이면 관리자 공통 키워드 |
| keyword | varchar | 키워드 |
| normalized_keyword | varchar | 정규화 키워드 |
| category_code | varchar | 내부 카테고리 |
| priority | varchar | HIGH/MEDIUM/LOW |
| status | varchar | ACTIVE/DELETED |
| analysis_status | varchar | PENDING/ANALYZED/FAILED |
| last_analyzed_at | timestamp | 마지막 분석 시각 |
| created_at | timestamp | 생성일 |

### shopping_price_snapshot

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 스냅샷 ID |
| keyword_id | bigint FK | 키워드 |
| base_date | date | 기준일 |
| total_results | bigint | 검색 결과 수 |
| min_price | int | 최저가 |
| max_price | int | 최고가 |
| avg_price | int | 평균가 |
| raw_json | jsonb | 원문 일부 |
| created_at | timestamp | 생성일 |

제약:

- keyword_id + base_date unique

### shopping_top_item

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 상위 상품 ID |
| snapshot_id | bigint FK | 쇼핑 검색 스냅샷 |
| item_rank | int | 검색 결과 순위 |
| title | varchar | 상품명 |
| link | text | 상품 링크 |
| image | text | 이미지 URL |
| lprice | int | 최저가 |
| hprice | int | 최고가 |
| mall_name | varchar | 쇼핑몰명 |
| product_id | varchar | 네이버 상품 ID |
| product_type | varchar | 상품 타입 |
| brand | varchar | 브랜드 |
| maker | varchar | 제조사 |
| category1 | varchar | 대분류 |
| category2 | varchar | 중분류 |
| category3 | varchar | 소분류 |
| category4 | varchar | 세분류 |
| created_at | timestamp | 생성일 |

### trend_snapshot

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 스냅샷 ID |
| keyword_id | bigint FK | 키워드 |
| period | date | 기준 기간 |
| time_unit | varchar | date/week/month |
| ratio | numeric | 상대 클릭 비율 |
| created_at | timestamp | 생성일 |

제약:

- keyword_id + period + time_unit unique

trend_score 계산:

```text
trend_delta_7d = latest_ratio - ratio_at_or_before(latest_period - 7 days)
trend_delta_30d = latest_ratio - ratio_at_or_before(latest_period - 30 days)
trend_score = round(clamp_positive(trend_delta_7d, 0, 100) * 0.15
                  + clamp_positive(trend_delta_30d, 0, 100) * 0.15)
```

- 점수 범위는 0~30으로 제한한다.
- 기준일에 정확한 ratio가 없으면 그 이전의 가장 가까운 period를 사용한다.
- 데이터랩 ratio는 검색 클릭 추이 기반이며 실제 판매량이 아니라는 warning reason을 포함한다.

### wholesale_file

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 업로드 파일 ID |
| user_id | bigint FK | 파일 소유 사용자 |
| source_name | varchar nullable | 도매처명 |
| original_filename | varchar | 원본 파일명 |
| stored_path | varchar | 서버 내부 저장 경로. API 응답에는 노출하지 않는다. |
| file_size | bigint | 파일 크기 |
| requested_encoding | varchar | AUTO/UTF_8/CP949 |
| detected_encoding | varchar | UTF_8/CP949 |
| row_count | int | 헤더 제외 데이터 행 수 |
| detected_columns | text | 감지된 헤더 컬럼 |
| mapping_product_name | varchar nullable | 상품명 매핑 컬럼 |
| mapping_supply_price | varchar nullable | 공급가 매핑 컬럼 |
| mapping_shipping_fee | varchar nullable | 배송비 매핑 컬럼 |
| mapping_category | varchar nullable | 카테고리 매핑 컬럼 |
| mapping_product_url | varchar nullable | 상품 URL 매핑 컬럼 |
| status | varchar | UPLOADED/MAPPED/PARSED/FAILED |
| created_at | timestamp | 생성일 |
| updated_at | timestamp | 수정일 |

### wholesale_product

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 상품 ID |
| file_id | bigint FK | 업로드 파일 |
| row_no | int | CSV 행 번호 |
| product_name | varchar | 상품명 |
| normalized_name | varchar | 정규화명 |
| supply_price | int | 공급가 |
| shipping_fee | int | 배송비 |
| source_category | varchar | 원본 카테고리 |
| product_url | text | 원본 URL |
| parse_status | varchar | PARSED/INVALID |
| error_message | text | 오류 내용 |

### product_candidate

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 후보 ID |
| user_id | bigint FK | 후보 소유 사용자 |
| source_type | varchar | KEYWORD/CSV/API |
| name | varchar | 후보명 |
| keyword_id | bigint nullable | 연결 키워드 |
| wholesale_product_id | bigint nullable | 연결 도매 상품 |
| category_code | varchar | 내부 카테고리 |
| expected_sale_price | int | 예상 판매가 |
| supply_price | int nullable | 공급가 |
| shipping_fee | int nullable | 배송비 |
| expected_margin_rate | numeric | 예상 마진율 |
| grade | varchar | RECOMMENDED/REVIEW/HOLD/EXCLUDED |
| status | varchar | ACTIVE/SAVED/EXCLUDED |
| created_at | timestamp | 생성일 |
| updated_at | timestamp | 수정일 |

### candidate_score

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 후보 점수 ID |
| candidate_id | bigint FK unique | 상품 후보 |
| trend_score | int | 검색 클릭 추이 점수 |
| competition_score | int | 경쟁 강도 점수 |
| margin_score | int | 예상 마진 점수 |
| price_band_score | int | 가격대 점수 |
| supply_score | int | 공급 데이터 신뢰 점수 |
| risk_penalty | int | 위험 카테고리 감점 |
| overall_score | int | 최종 점수 |
| grade | varchar | RECOMMENDED/REVIEW/HOLD/EXCLUDED |
| risk_level | varchar | LOW/CAUTION/EXCLUDED |
| reasons | text | 점수 산정 사유 |
| warnings | text | 검토 주의사항 |
| created_at | timestamp | 생성일 |

### alert_rule

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 알림 조건 ID |
| user_id | bigint FK | 조건 소유 사용자 |
| name | varchar | 조건명 |
| min_score | int | 최소 후보 점수 |
| min_margin_rate | numeric | 최소 예상 마진율 |
| category_codes | text | 대상 카테고리 목록 |
| risk_excluded | boolean | 위험 제외 후보 필터 여부 |
| frequency | varchar | DAILY_SUMMARY/WEEKLY_SUMMARY |
| active | boolean | 활성 여부 |
| created_at | timestamp | 생성일 |
| updated_at | timestamp | 수정일 |

### alert

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 알림 ID |
| user_id | bigint FK | 알림 소유 사용자 |
| rule_id | bigint FK | 생성 기준 조건 |
| candidate_id | bigint FK | 연결 후보 |
| type | varchar | CANDIDATE_SCORE |
| status | varchar | UNREAD/READ |
| title | varchar | 알림 제목 |
| message | varchar | 알림 내용 |
| created_at | timestamp | 생성일 |
| read_at | timestamp nullable | 읽은 시각 |

### batch_job_history

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | 배치 실행 이력 ID |
| job_type | varchar | SHOPPING_SEARCH_DAILY 등 배치 종류 |
| trigger_type | varchar | MANUAL/SCHEDULED |
| status | varchar | RUNNING/SUCCESS/PARTIAL_SUCCESS/FAILED |
| target_count | int | 실행 대상 수 |
| success_count | int | 성공 대상 수 |
| failure_count | int | 실패 대상 수 |
| started_at | timestamp | 시작 시각 |
| finished_at | timestamp nullable | 종료 시각 |
| error_message | varchar nullable | 실패 요약 |
| created_at | timestamp | 생성일 |

### api_call_log

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | API 호출 로그 ID |
| provider | varchar | 외부 API 제공자 |
| api_name | varchar | API 이름 |
| keyword_id | bigint nullable | 연결 키워드 |
| base_date | date | 기준일 |
| status | varchar | SUCCESS/FAILED |
| http_status | int | HTTP 상태 코드 |
| error_code | varchar nullable | 내부 오류 코드 |
| error_message | varchar nullable | 오류 메시지 |
| created_at | timestamp | 생성일 |

---

## 9. 추천점수 설계

## 9.1 점수 구성

```text
overall_score = trend_score + competition_score + margin_score + price_band_score + supply_score + risk_penalty
```

| 항목 | 배점 | 설명 |
|---|---:|---|
| trend_score | 0~30 | 최근 7일/30일 검색 클릭 추이 상승 |
| competition_score | 0~25 | 검색 결과 수, 상위 가격 분포, 가격 경쟁 강도 |
| margin_score | 0~30 | 예상 마진율/마진액 |
| price_band_score | 0~10 | 초보 셀러 적정 판매가 9,900~49,900원 |
| supply_score | 0~5 | 배송비/공급가 유효성 |
| risk_penalty | 0~-40 | 규제/인증/CS 위험 |

세부 계산:

- trend_score는 DataLab trend_score 계산 결과를 0~30으로 제한한다.
- competition_score는 검색 결과 수가 적을수록 높게 주고, 상위 가격대 편차가 크면 감점한다.
- margin_score는 예상 판매가, 공급가, 배송비 기준 예상 마진율로 계산한다.
- price_band_score는 9,900~49,900원을 10점, 인접 가격대를 5점으로 계산한다.
- supply_score는 공급가와 배송비가 유효하면 5점으로 계산한다.
- risk_penalty는 안전 0점, 주의 -15점, 제외 -40점으로 계산한다.
- 위험 카테고리 제외 룰에 매칭되면 overall_score와 별개로 grade를 EXCLUDED로 강제한다.
- reasons/warnings는 계산 결과와 함께 구조화해 저장 대상으로 전달한다.

## 9.2 등급

| 점수 | 등급 | 의미 |
|---:|---|---|
| 80~100 | RECOMMENDED | 추천 검토 후보 |
| 65~79 | REVIEW | 검토 후보 |
| 50~64 | HOLD | 보류 |
| 0~49 | HOLD | 낮은 우선순위 |

EXCLUDED 등급은 점수만으로 부여하지 않고, 위험 카테고리 제외 룰에 매칭된 경우에만 강제한다.

## 9.3 위험 카테고리 기본 제외

| 카테고리 | 기본 처리 | 사유 |
|---|---|---|
| 식품 | 제외 | 신고/표시/보관 리스크 |
| 건강기능식품 | 제외 | 광고/인허가 리스크 |
| 화장품 | 제외 | 책임판매/표시 리스크 |
| 의료기기 | 제외 | 허가/광고 리스크 |
| 전기/배터리/충전기 | 제외 | KC/안전 인증 리스크 |
| 어린이제품 | 제외 | 어린이제품 안전관리 리스크 |
| 의류/신발 | 보류 | 사이즈/반품 리스크 |
| 대형가구 | 보류 | 배송/파손/반품 리스크 |

---

## 10. API 호출/캐싱 전략

## 10.1 원칙

1. 화면 요청 시 외부 API 직접 호출 금지
2. 키워드별 하루 1회 이하 호출
3. 외부 API 결과는 snapshot으로 저장
4. 실패 시 마지막 성공 snapshot 사용
5. 배치 이력과 API 호출 로그 저장

## 10.2 데이터랩 한도 대응

네이버 데이터랩 쇼핑인사이트 API는 하루 1,000회 호출 한도가 있으므로 다음 제한을 둔다.

| 단계 | 키워드 수 | 전략 |
|---|---:|---|
| v0.1 | 300~500 | 관리자 키워드만 일일 분석 |
| v0.2 | 800~1,000 | 사용자 키워드 포함, 우선순위 적용 |
| v0.3 | 1,000 초과 | LOW 키워드는 주간 순환 분석 |
| 유료화 이후 | 플랜 제한 | FREE 3개, BASIC 30개, PRO 100개 |

---

## 11. 배치 설계

| Job | Cron 예시 | 설명 |
|---|---|---|
| SHOPPING_SEARCH_DAILY | 30 6 * * * | 쇼핑 검색 가격대 수집 |
| DATALAB_TREND_DAILY | 0 7 * * * | 데이터랩 트렌드 수집 |
| SCORE_RECALC_DAILY | 30 7 * * * | 점수 재계산 |
| ALERT_GENERATE_DAILY | 0 8 * * * | 알림 생성 |
| OLD_FILE_CLEANUP | 0 3 * * SUN | 오래된 CSV 원본 삭제 |

---

## 12. 보안 설계

1. API Secret은 환경변수 또는 Secret Manager로 관리한다.
2. 프론트엔드에 외부 API 키를 포함하지 않는다.
3. CSV 원본 파일은 접근 권한이 있는 서버 내부 저장소에만 보관한다.
4. 사용자 업로드 파일은 30일 보관 후 삭제 가능하도록 설계한다.
5. 로그에서 비밀번호, JWT, API Secret, 개인정보를 마스킹한다.
6. 관리자 API는 IP 제한 또는 별도 관리자 권한을 둔다.

---

## 13. 운영/모니터링

MVP부터 다음 정보를 저장한다.

| 항목 | 목적 |
|---|---|
| api_call_log | 외부 API 호출량/실패 추적 |
| batch_job_history | 배치 성공/실패 확인 |
| external_api_error_log | 403/429/500 등 원인 분석 |
| keyword_analysis_summary | 최신 분석 상태 표시 |

관리자 대시보드 최소 지표:

```text
오늘 데이터랩 호출 수 / 1000
오늘 쇼핑 검색 호출 수 / 25000
배치 성공 여부
실패 키워드 수
알림 생성 수
유료 사용자 수
```

---

## 14. 배포 설계

## 14.1 로컬

```text
React dev server
Spring Boot API
PostgreSQL Docker
```

## 14.2 운영 초기

```text
Nginx
React 정적 빌드
Spring Boot Docker container
PostgreSQL
Docker Compose
```

## 14.3 확장 시

```text
Spring Boot API 서버 분리
Batch worker 분리
Managed PostgreSQL
Object Storage for CSV
Redis cache 선택 도입
```

---

## 15. 앱인토스 Lite 설계

앱인토스는 유입/검증 채널이다. Web SaaS 본체로 사용자를 강제로 보내는 광고판으로 만들지 않는다.

Lite 제공 기능:

1. 오늘 추천 후보 1~3개
2. 빠른 마진 계산
3. 관심 키워드 3개 등록
4. 제한적 알림 목록
5. BASIC/PRO 유료 권한 안내

정책 원칙:

- 앱인토스 내 설정한 기능은 미니앱 안에서 완결한다.
- 외부 결제창으로 보내지 않는다.
- 자사 앱 설치 유도 문구를 사용하지 않는다.

---

## 16. 개발 로드맵

| 단계 | 목표 | 산출 기능 |
|---|---|---|
| v0.1 | 키워드/쇼핑 가격 분석 | 키워드 CRUD, 쇼핑 검색 API, 캐싱, 상세 화면 |
| v0.2 | 트렌드 분석 | 데이터랩 API, trend snapshot, trend score |
| v0.3 | 도매 CSV/마진 | CSV 업로드, 파싱, 마진 계산, 후보 생성 |
| v0.4 | 알림 | 알림 조건, 알림 생성, 목록 |
| v1.0 | 앱인토스 Lite | 토스용 화면, 마진 계산, 추천 카드 |
| v1.5 | 스마트스토어 연동 | 상품/주문/정산/수수료 API |
| v2.0 | 고도화 | 도매 API, AI 요약, 리포트 |

---

## 17. 주요 리스크와 대응

| 리스크 | 영향 | 대응 |
|---|---|---|
| 데이터랩 호출 한도 | 분석 확장 제한 | 배치/캐시/우선순위/플랜 제한 |
| 추천 품질 부족 | 신뢰 하락 | 추천이 아닌 검토 후보로 표현 |
| 도매 API 승인 지연 | 공급가 자동화 지연 | 초기 CSV 업로드 |
| 앱인토스 정책 위반 | 출시 반려 | Lite 기능 완결, 외부 이동 제한 |
| Codex 범위 폭발 | 코드 품질 저하 | Task/PR 단위 개발 |
| 수익화 실패 | 지속 어려움 | 저장/알림/CSV/대량분석을 유료화 |

---

## 18. Definition of Done

MVP v0.1 완료 기준:

1. 키워드 등록/수정/삭제 가능
2. 네이버 쇼핑 검색 API 결과를 서버 배치/수동 실행으로 저장
3. 같은 키워드는 하루 1회 이상 외부 API 호출하지 않음
4. 키워드 상세에서 최저가/평균가/상위상품을 조회 가능
5. API 실패 시 사용자 화면에 명확한 오류 또는 마지막 캐시 표시
6. 백엔드 단위 테스트 통과
7. 프론트 빌드 성공
8. Docker Compose로 로컬 DB 실행 가능

---

## 19. 참고 공식 문서

- 네이버 쇼핑 검색 API: https://developers.naver.com/docs/serviceapi/search/shopping/shopping.md
- 네이버 데이터랩 쇼핑인사이트 API: https://developers.naver.com/docs/serviceapi/datalab/shopping/shopping.md
- 앱인토스 WebView SDK 연동: https://developers-apps-in-toss.toss.im/tutorials/webview.html
- 앱인토스 서비스 오픈 정책: https://developers-apps-in-toss.toss.im/intro/guide.html
- 앱인토스 인앱 결제: https://developers-apps-in-toss.toss.im/iap/intro.html
- 네이버 커머스API 소개: https://apicenter.commerce.naver.com/docs/introduction
