# 07. Codex 개발 실행 계획 v2

- 문서 버전: v0.2
- 기준일: 2026-07-03
- 전제: 프로젝트 골조는 이미 생성되어 있고 Docker, DB, Backend, Frontend가 기동된다.
- 목적: Codex Pro가 효율적으로 작업할 수 있도록 기능을 작은 PR 단위로 쪼갠다.

---

## 1. Codex 작업 원칙

## 1.1 한 번에 하나의 PR

나쁜 지시:

```text
셀러레이더 v0.1 전체 만들어줘.
```

좋은 지시:

```text
keyword 테이블, 엔티티, Repository, Service, Controller, 테스트까지만 구현해줘.
프론트 변경은 하지 마.
```

---

## 1.2 작업 전 Codex가 먼저 요약하게 할 것

모든 작업 프롬프트에 다음을 포함한다.

```text
구현 전에 먼저 아래를 요약해줘.
1. 수정할 파일 목록
2. 추가할 DB migration
3. 추가/변경할 API endpoint
4. 테스트 계획
5. 이번 작업에서 제외할 범위
그 다음 구현을 진행해줘.
```

---

## 1.3 작업 후 Codex가 보고하게 할 것

```text
작업 후 아래를 보고해줘.
1. 변경 파일 목록
2. 실행한 검증 명령어
3. 테스트/빌드 결과
4. 알려진 제한사항
5. 다음 작업 제안
```

---

## 1.4 중단 기준

Codex가 아래 상황을 만나면 구현하지 말고 확인을 요청해야 한다.

```text
마이그레이션이 기존 데이터를 삭제할 수 있음
외부 API 정책이 불명확함
secret을 코드에 넣어야 함
AGENTS.md와 충돌함
요청 범위 밖 기능을 건드려야 함
디자인 전면 수정이 필요함
```

---

## 2. 현재 상태 기준 작업 큐

현재 완료:

```text
Task 000. 프로젝트 골조 생성
Task 001. Docker/PostgreSQL 연결
Task 002. Backend/Frontend 기본 기동
```

이제 다음부터 진행한다.

---


# EPIC P0. 기반 구축

## Task P0-001. DB 상세 설계 반영 확인

### 목적

`docs/10_database_design.md`를 기준으로 P1~P3에서 사용할 테이블, 인덱스, 제약조건, migration 순서를 확인한다.

### Codex 프롬프트

```text
P1 기능 구현 전에 docs/10_database_design.md를 읽고 DB 적용 범위를 확인해줘.

구현하지 말고 먼저 아래를 요약해줘.
1. P1에서 필요한 테이블
2. P2에서 필요한 테이블
3. 최초 migration 파일 목록 제안
4. 기존 코드/DB와 충돌 가능성
5. 구현 전 확인이 필요한 사항

이 작업에서는 코드 수정 없이 분석 요약만 해줘.
```

### 완료 기준

```text
P1~P3 테이블과 migration 순서가 Codex 작업 계획에 반영됨
```

---

# EPIC P1. 키워드 레이더

## Task P1-001. Keyword DB/Domain 구현

### 목적

키워드 관리를 위한 backend domain을 만든다.

### Codex 프롬프트

```text
현재 프로젝트는 Docker, PostgreSQL, Spring Boot, React 기본 화면 기동까지 완료된 상태다.
이제 P1-001 작업으로 Keyword DB/Domain을 구현해줘.

구현 전에 먼저 아래를 요약해줘.
1. 수정할 파일 목록
2. 추가할 DB migration
3. 추가/변경할 API endpoint
4. 테스트 계획
5. 이번 작업에서 제외할 범위
그 다음 구현을 진행해줘.

요구사항:
1. keyword 테이블을 생성한다.
2. 필드는 id, user_id, keyword, category, active, last_analyzed_at, created_at, updated_at으로 한다.
3. 같은 user_id에서 같은 keyword가 중복 등록되지 않게 DB unique 제약 또는 service 검증을 둔다.
4. Keyword entity, repository, service를 만든다.
5. Controller는 아직 만들지 말고 service 단위 테스트까지 작성한다.
6. user_id는 인증 구현 전이므로 Long 또는 String userId로 단순 처리한다.
7. AGENTS.md 규칙을 따른다.

검증:
- cd backend && ./gradlew test

작업 후 아래를 보고해줘.
1. 변경 파일 목록
2. 실행한 검증 명령어
3. 테스트/빌드 결과
4. 알려진 제한사항
5. 다음 작업 제안
```

### 완료 기준

```text
keyword table migration 존재
Keyword service 테스트 통과
중복 키워드 등록 방지 테스트 존재
```

### 제외

```text
프론트 화면
네이버 API
트렌드 분석
```

---

## Task P1-002. Keyword REST API 구현

### 목적

키워드 등록/조회/수정/삭제 API를 만든다.

### Codex 프롬프트

```text
P1-002 작업으로 Keyword REST API를 구현해줘.

구현 전에 먼저 아래를 요약해줘.
1. 수정할 파일 목록
2. 추가/변경할 API endpoint
3. 테스트 계획
4. 이번 작업에서 제외할 범위

요구사항:
1. KeywordController를 추가한다.
2. 아래 API를 구현한다.
   - POST /api/keywords
   - GET /api/keywords
   - GET /api/keywords/{id}
   - PATCH /api/keywords/{id}
   - DELETE /api/keywords/{id}
3. 인증 전이므로 userId는 임시로 X-USER-ID 헤더에서 받는다.
4. 요청/응답 DTO를 분리한다.
5. active=false 처리 또는 soft delete 방식으로 삭제한다.
6. validation 오류와 중복 오류는 일관된 error response로 반환한다.
7. Controller test 또는 integration test를 작성한다.
8. Swagger/OpenAPI가 있으면 endpoint가 보이게 한다.

검증:
- cd backend && ./gradlew test

제외:
- 프론트 화면
- 인증/JWT
- 네이버 API
```

### 완료 기준

```text
Swagger 또는 API test로 CRUD 확인 가능
에러 응답 일관성 유지
중복 등록 시 409 또는 정의된 오류 반환
```

---

## Task P1-003. Keyword Frontend 화면 구현

### 목적

키워드를 등록하고 목록을 보는 화면을 구현한다.

### Codex 프롬프트

```text
P1-003 작업으로 Keyword 관리 화면을 구현해줘.

구현 전에 먼저 아래를 요약해줘.
1. 수정할 파일 목록
2. 추가할 frontend route/component
3. backend API 연동 방식
4. 테스트/빌드 계획
5. 이번 작업에서 제외할 범위

요구사항:
1. /keywords 라우트를 추가한다.
2. 키워드 등록 폼을 만든다.
3. 키워드 목록 테이블을 만든다.
4. 등록/목록조회/삭제 API를 연동한다.
5. X-USER-ID는 임시로 frontend api client에서 고정값 또는 dev 설정값으로 보낸다.
6. 디자인은 최소 수준으로 한다.
7. AppShell, PageHeader, DataTable 같은 공통 구조가 있으면 재사용한다.
8. 나중에 디자인 대개편이 쉬우도록 하드코딩 스타일을 최소화한다.

검증:
- cd frontend && npm run build
- docker compose 상태에서 화면에서 등록/조회 가능

제외:
- 최종 디자인 완성
- 네이버 API 분석 버튼
- 추천점수
```

### 완료 기준

```text
브라우저에서 키워드 등록 가능
등록 후 목록 갱신
build 성공
```

---

# EPIC P2. 네이버 쇼핑 스냅샷

## Task P2-001. NaverShoppingClient 구현

### 목적

네이버 쇼핑 검색 API client를 backend에 추가한다.

### Codex 프롬프트

```text
P2-001 작업으로 NaverShoppingClient를 구현해줘.

구현 전에 먼저 아래를 요약해줘.
1. 수정할 파일 목록
2. 설정 파일 변경사항
3. 외부 API DTO 구조
4. 테스트 계획
5. 이번 작업에서 제외할 범위

요구사항:
1. backend에 shopping package를 추가한다.
2. NaverShoppingClient를 구현한다.
3. client id/client secret은 환경변수 또는 application config로만 주입한다.
4. 네이버 쇼핑 검색 응답 DTO를 만든다.
5. title, link, image, lprice, hprice, mallName, productId, brand, maker, category1~4를 매핑한다.
6. 테스트에서는 실제 네이버 API를 호출하지 않는다.
7. MockWebServer 또는 WireMock 기반 테스트를 작성한다.
8. API 오류, 429, 5xx 응답을 처리한다.

검증:
- cd backend && ./gradlew test

제외:
- DB 저장
- 화면 표시
- 실제 API key commit
```

### 완료 기준

```text
mock 응답 parsing 테스트 통과
secret 하드코딩 없음
```

---

## Task P2-002. Shopping Snapshot 저장 구현

### 목적

네이버 쇼핑 검색 결과를 snapshot 테이블에 저장한다.

### Codex 프롬프트

```text
P2-002 작업으로 네이버 쇼핑 검색 결과 snapshot 저장 기능을 구현해줘.

요구사항:
1. shopping_search_snapshot 테이블을 생성한다.
2. shopping_item_snapshot 테이블을 생성한다.
3. api_call_log 테이블이 없으면 추가한다.
4. 키워드 분석 service를 만든다.
5. 분석 시 NaverShoppingClient 결과를 snapshot과 item으로 저장한다.
6. 같은 keyword_id + search_date 조합에 성공 snapshot이 있으면 외부 API를 재호출하지 않고 캐시를 반환한다.
7. 상품 이미지는 다운로드하지 말고 image_url만 저장한다.
8. API 호출 성공/실패를 api_call_log에 저장한다.
9. service 테스트를 작성한다.

검증:
- cd backend && ./gradlew test

제외:
- 프론트 화면
- 경쟁강도 고도화
- 데이터랩
```

### 완료 기준

```text
snapshot 저장 테스트 통과
중복 호출 방지 테스트 통과
api_call_log 저장 확인
```

---

## Task P2-003. Keyword Analysis API 구현

### 목적

프론트에서 키워드 분석을 요청하고 결과를 조회할 수 있게 한다.

### Codex 프롬프트

```text
P2-003 작업으로 키워드 분석 API를 구현해줘.

요구사항:
1. POST /api/keywords/{id}/analyze/shopping API를 만든다.
2. GET /api/keywords/{id}/shopping-snapshot/latest API를 만든다.
3. 응답에는 가격 요약과 item 상위 10개를 포함한다.
4. 가격 요약에는 minPrice, maxPrice, avgPrice, totalCount, fetchedAt을 포함한다.
5. item에는 title, productUrl, imageUrl, lowPrice, mallName, category1~4, rankNo를 포함한다.
6. 같은 날짜 snapshot이 있으면 캐시 결과임을 표시한다.
7. integration test를 작성한다.

검증:
- cd backend && ./gradlew test

제외:
- 프론트 화면
- 데이터랩
- 도매 CSV
```

---

## Task P2-004. Shopping Product Cards Frontend 구현

### 목적

키워드 상세 화면에서 상품 카드 10개를 보여준다.

### Codex 프롬프트

```text
P2-004 작업으로 키워드 상세 화면과 상품 카드 UI를 구현해줘.

요구사항:
1. /keywords/:id 라우트를 추가한다.
2. 키워드 상세 화면에 분석 실행 버튼을 추가한다.
3. 최신 shopping snapshot을 조회해 가격 요약을 보여준다.
4. 상품 카드 10개를 표시한다.
5. 상품 카드에는 이미지, 상품명, 최저가, 몰명, 카테고리, 원본 링크를 표시한다.
6. imageUrl이 없거나 로드 실패하면 placeholder를 보여준다.
7. 디자인은 기본 수준으로 하되 ProductCard 컴포넌트를 분리한다.
8. 나중 디자인 대개편을 위해 스타일 하드코딩을 최소화한다.

검증:
- cd frontend && npm run build
- 브라우저에서 키워드 상세 진입 가능

제외:
- 최종 디자인
- 추천점수
- 도매 매칭
```

---

# EPIC P3. 가격/경쟁 분석

## Task P3-001. Competition Analyzer 구현

### 목적

가격대와 결과 수 기준으로 경쟁강도를 계산한다.

### Codex 프롬프트

```text
P3-001 작업으로 CompetitionAnalyzer를 구현해줘.

요구사항:
1. competitionLevel을 LOW/MEDIUM/HIGH enum으로 만든다.
2. 기본 기준은 다음과 같다.
   - totalCount >= 10000: HIGH
   - totalCount >= 3000: MEDIUM
   - otherwise: LOW
3. snapshot 생성 시 competitionLevel을 저장한다.
4. 가격 평균 계산에서 숫자가 없거나 0인 item은 제외한다.
5. 경계값 단위 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P3-002. 가격/경쟁강도 화면 표시

### Codex 프롬프트

```text
P3-002 작업으로 키워드 목록과 상세 화면에 가격/경쟁강도를 표시해줘.

요구사항:
1. 키워드 목록에 최신 minPrice, avgPrice, competitionLevel, lastAnalyzedAt을 표시한다.
2. 키워드 상세에 가격 요약 카드와 경쟁강도 badge를 표시한다.
3. 값이 없으면 '분석 전' 상태를 보여준다.
4. StatusBadge 컴포넌트를 분리한다.

검증:
- cd frontend && npm run build
```

---

# EPIC P4. 데이터랩 트렌드 분석

## Task P4-001. Trend Snapshot DB/Domain 구현

### Codex 프롬프트

```text
P4-001 작업으로 데이터랩 트렌드 저장 구조를 구현해줘.

요구사항:
1. trend_snapshot 테이블을 추가한다.
2. batch_job_history 테이블이 없으면 추가한다.
3. keyword별 date, periodType, ratio, source를 저장한다.
4. TrendSnapshot entity/repository/service를 구현한다.
5. 저장/조회 service 테스트를 작성한다.

검증:
- cd backend && ./gradlew test

제외:
- 실제 데이터랩 API client
- batch scheduler
```

---

## Task P4-002. NaverDataLabClient 구현

### Codex 프롬프트

```text
P4-002 작업으로 NaverDataLabClient를 구현해줘.

요구사항:
1. DataLab Shopping Insight API client를 만든다.
2. 한 요청에 최대 5개 키워드 묶음으로 요청할 수 있게 설계한다.
3. client id/secret은 환경변수로 주입한다.
4. 실제 API 호출 테스트는 금지하고 MockWebServer 또는 WireMock 테스트를 작성한다.
5. API 오류와 429를 처리한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P4-003. Daily Trend Batch 구현

### Codex 프롬프트

```text
P4-003 작업으로 일일 트렌드 수집 batch를 구현해줘.

요구사항:
1. active keyword를 대상으로 배치 수집한다.
2. API 한도를 고려해 하루 처리 대상 개수를 설정값으로 제한한다.
3. 실행 이력을 batch_job_history에 저장한다.
4. keyword별 실패는 전체 batch 실패로 만들지 말고 failure_count에 기록한다.
5. 수동 실행용 admin/dev endpoint를 만들되 production에서 보호 가능하게 분리한다.
6. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

# EPIC P5. 도매 CSV/XLSX 업로드

## Task P5-001. Wholesale Product DB/Domain 구현

### Codex 프롬프트

```text
P5-001 작업으로 wholesale_product 저장 구조를 구현해줘.

요구사항:
1. wholesale_upload_job 테이블을 추가한다.
2. wholesale_product 테이블을 추가한다.
3. 표준 컬럼은 source, externalProductId, productName, supplyPrice, shippingFee, imageUrl, productUrl, category, brand, maker, optionName, stockStatus, isSoldOut, memo로 한다.
4. 업로드 job과 product가 연결되게 한다.
5. service 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P5-002. CSV/XLSX 파일 파서 구현

### Codex 프롬프트

```text
P5-002 작업으로 CSV/XLSX 파서를 구현해줘.

요구사항:
1. .csv, .xlsx 파일을 지원한다.
2. 첫 번째 행을 header로 읽는다.
3. 숫자 컬럼에서 콤마, 원, 공백을 제거하고 BigDecimal 또는 Long으로 변환한다.
4. 파싱 결과는 저장하지 않고 preview DTO로 반환하는 service를 만든다.
5. 잘못된 파일, 빈 파일, header 없는 파일 오류를 처리한다.
6. 단위 테스트를 작성한다.

검증:
- cd backend && ./gradlew test

제외:
- 프론트 업로드 화면
- 실제 DB 저장
```

---

## Task P5-003. 컬럼 매핑/저장 API 구현

### Codex 프롬프트

```text
P5-003 작업으로 도매 CSV 업로드 preview와 컬럼 매핑 저장 API를 구현해줘.

요구사항:
1. POST /api/wholesale-uploads/preview API를 만든다.
2. POST /api/wholesale-uploads/{uploadId}/confirm API를 만든다.
3. 사용자가 productName, supplyPrice, shippingFee, imageUrl, productUrl, category 컬럼을 매핑할 수 있게 한다.
4. 필수 매핑은 productName, supplyPrice다.
5. 저장 결과로 successCount, failureCount, failureReasons를 반환한다.
6. 업로드 파일 크기 제한과 확장자 검증을 한다.
7. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P5-004. Wholesale Upload Frontend 구현

### Codex 프롬프트

```text
P5-004 작업으로 도매 CSV/XLSX 업로드 화면을 구현해줘.

요구사항:
1. /wholesale/uploads 라우트를 추가한다.
2. 파일 선택 후 preview API를 호출한다.
3. header 목록을 보여주고 표준 컬럼과 매핑할 수 있게 한다.
4. confirm API 호출 후 성공/실패 건수를 보여준다.
5. 실패 사유 목록을 표시한다.
6. 디자인은 기본 수준으로 하되 나중 디자인 대개편을 고려해 컴포넌트를 분리한다.

검증:
- cd frontend && npm run build
```

---

# EPIC P6. 마진/상품 검토 점수

## Task P6-001. Margin Calculator 구현

### Codex 프롬프트

```text
P6-001 작업으로 예상 마진 계산기를 구현해줘.

요구사항:
1. MarginCalculator service를 만든다.
2. 입력값은 salePrice, supplyPrice, shippingFee, platformFeeRate, adCost, couponCost, extraCost다.
3. 출력값은 totalCost, expectedProfit, expectedMarginRate, recommendedSalePrice다.
4. 목표 마진율 기준 권장 판매가를 계산한다.
5. 경계값 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P6-002. Candidate Scoring 구현

### Codex 프롬프트

```text
P6-002 작업으로 상품 검토 점수 계산 로직을 구현해줘.

요구사항:
1. CandidateScoreCalculator service를 만든다.
2. 점수 구성은 trendScore, priceScore, marginScore, competitionScore, riskPenalty로 분리한다.
3. 총점은 0~100 범위로 clamp한다.
4. 80점 이상 RECOMMENDED_REVIEW, 65점 이상 REVIEW, 그 외 HOLD로 분류한다.
5. scoreReason breakdown을 저장/반환한다.
6. '판매 성공 점수' 같은 표현은 사용하지 않는다.
7. 단위 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P6-003. Candidate 화면 구현

### Codex 프롬프트

```text
P6-003 작업으로 상품 검토 후보 화면을 구현해줘.

요구사항:
1. /candidates 라우트를 추가한다.
2. 상품 검토 후보 목록을 표시한다.
3. 각 후보에는 상품명, 이미지, 예상 판매가, 공급가, 예상 마진율, 상품 검토 점수, 상태를 표시한다.
4. 점수 breakdown을 펼쳐볼 수 있게 한다.
5. 화면 문구는 '검토 후보' 중심으로 작성한다.
6. 디자인은 기능 위주로 최소화한다.

검증:
- cd frontend && npm run build
```

---

# EPIC P7. 알림/배치

## Task P7-001. Alert Rule/Notification DB 구현

### Codex 프롬프트

```text
P7-001 작업으로 알림 조건과 알림 저장 구조를 구현해줘.

요구사항:
1. alert_rule 테이블을 추가한다.
2. notification 테이블을 추가한다.
3. 추천점수 기준, 마진율 기준, 트렌드 상승 기준을 저장할 수 있게 한다.
4. 알림 중복 방지 key를 둔다.
5. service 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P7-002. Notification 생성 Batch 구현

### Codex 프롬프트

```text
P7-002 작업으로 알림 생성 batch를 구현해줘.

요구사항:
1. 후보 점수와 alert_rule을 비교해 notification을 생성한다.
2. 동일 후보/동일 조건/동일 날짜 중복 알림을 방지한다.
3. batch_job_history에 실행 이력을 저장한다.
4. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

# EPIC P8. Web SaaS 하드닝

## Task P8-001. Auth 최소 구현

### Codex 프롬프트

```text
P8-001 작업으로 MVP 수준의 사용자 인증을 구현해줘.

요구사항:
1. user 테이블을 만든다.
2. 이메일/비밀번호 기반 회원가입/로그인을 구현한다.
3. 비밀번호는 BCrypt 등 안전한 방식으로 저장한다.
4. JWT 발급/검증을 구현한다.
5. 기존 X-USER-ID 임시 구조를 인증 사용자 기반으로 교체할 준비를 한다.
6. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P8-002. User Data Isolation 적용

### Codex 프롬프트

```text
P8-002 작업으로 사용자별 데이터 분리를 적용해줘.

요구사항:
1. keyword, upload, candidate, notification 조회가 인증 사용자 기준으로만 동작하게 한다.
2. 다른 user의 id로 접근하면 403 또는 404를 반환한다.
3. 기존 임시 X-USER-ID 제거 또는 dev profile에서만 허용한다.
4. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

# EPIC P9. 스마트스토어 API 1차 연동

## Task P9-001. SmartStore API 설계 skeleton

### Codex 프롬프트

```text
P9-001 작업으로 SmartStore API 연동 skeleton을 구현해줘.

요구사항:
1. smartstore package를 만든다.
2. SmartStoreConnection entity를 만든다.
3. 사용자별 연결 상태를 저장한다.
4. 실제 외부 API 호출은 아직 구현하지 않고 interface와 mock adapter만 만든다.
5. 토큰/secret 저장은 plain text로 하지 않도록 TODO와 구조를 명시한다.
6. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test

제외:
- 실제 네이버 커머스API 호출
- 주문/정산 연동
```

---

## Task P9-002. SmartStore Product Sync 1차

### Codex 프롬프트

```text
P9-002 작업으로 스마트스토어 상품 동기화 1차를 구현해줘.

요구사항:
1. SmartStoreProduct entity를 만든다.
2. 외부 API client는 mock 가능하게 adapter 구조로 구현한다.
3. 상품명, 판매가, 상태, sourceProductId를 저장한다.
4. 수동 동기화 API를 만든다.
5. 동기화 이력을 저장한다.
6. 테스트에서는 실제 API를 호출하지 않는다.

검증:
- cd backend && ./gradlew test
```

---

# EPIC P10. 스마트스토어 내 상품 마진 감시

## Task P10-001. Store Product Cost Mapping

### Codex 프롬프트

```text
P10-001 작업으로 스마트스토어 상품과 원가 데이터를 매핑하는 기능을 구현해줘.

요구사항:
1. store_product_cost_mapping 테이블을 만든다.
2. SmartStoreProduct와 wholesaleProduct 또는 수동 원가를 연결한다.
3. 사용자가 상품별 공급가, 배송비, 기타 비용을 직접 입력할 수 있게 한다.
4. 예상 마진을 계산한다.
5. 테스트를 작성한다.

검증:
- cd backend && ./gradlew test
```

---

## Task P10-002. Store Margin Risk Dashboard

### Codex 프롬프트

```text
P10-002 작업으로 내 상품 마진 위험 대시보드를 구현해줘.

요구사항:
1. /store/margins 화면을 만든다.
2. 내 스마트스토어 상품별 판매가, 원가, 예상 마진율, 위험 상태를 표시한다.
3. 위험/주의/안전 카운트를 표시한다.
4. 디자인은 기본 수준으로 하되 MetricCard, DataTable, StatusBadge를 재사용한다.

검증:
- cd frontend && npm run build
```

---

# EPIC P11. 주문/정산/수수료 연동

## Task P11-001. Settlement Skeleton

### Codex 프롬프트

```text
P11-001 작업으로 주문/정산/수수료 연동을 위한 skeleton을 구현해줘.

요구사항:
1. order, settlement, commission 관련 package/entity skeleton을 만든다.
2. 실제 외부 API 호출은 interface/mock adapter만 둔다.
3. 예상 손익과 실제 손익 비교를 위한 service interface를 정의한다.
4. 테스트 skeleton을 작성한다.

검증:
- cd backend && ./gradlew test
```

---

# EPIC P12. 디자인 대개편

## Task P12-001. Design Token Foundation

### Codex 프롬프트

```text
P12-001 작업으로 디자인 대개편 전 design token foundation을 추가해줘.

요구사항:
1. 색상, spacing, radius, shadow, typography token 구조를 만든다.
2. 기존 화면의 하드코딩 스타일을 가능한 token 참조로 교체한다.
3. 화면 레이아웃은 크게 바꾸지 않는다.
4. 시각 디자인 완성은 다음 task로 남긴다.

검증:
- cd frontend && npm run build
```

---

## Task P12-002. UI Component Consolidation

### Codex 프롬프트

```text
P12-002 작업으로 중복 UI 컴포넌트를 정리해줘.

요구사항:
1. MetricCard, DataTable, ProductCard, StatusBadge, EmptyState, ErrorState, LoadingState를 공통화한다.
2. feature별 화면은 공통 컴포넌트를 사용하게 교체한다.
3. 기능 로직은 변경하지 않는다.
4. API contract 변경 금지.

검증:
- cd frontend && npm run build
```

---

# EPIC P13. 앱인토스 Lite

## Task P13-001. Apps in Toss Lite Route Skeleton

### Codex 프롬프트

```text
P13-001 작업으로 앱인토스 Lite용 frontend route skeleton을 구현해줘.

요구사항:
1. /toss 라우트를 추가한다.
2. 빠른 마진 계산, 오늘의 검토 후보, 내 상품 위험 요약 영역을 만든다.
3. 외부 SaaS 강제 이동 문구를 넣지 않는다.
4. 아직 Toss SDK 연동은 하지 않는다.
5. 모바일 WebView 화면 비율을 고려한다.

검증:
- cd frontend && npm run build
```

---

# EPIC P14. 베타/수익화

## Task P14-001. Beta Readiness and Monetization Metrics

### Codex 프롬프트

```text
P14-001 작업으로 베타/수익화 준비 항목을 정리해줘.

요구사항:
1. 베타 사용자 운영 지표와 수익화 검증 지표를 정의한다.
2. 요금제 가설과 기능 제한 기준을 문서화한다.
3. 실제 결제 연동은 별도 task로 분리한다.
4. 운영 로그와 사용량 지표는 docs/10_database_design.md의 P14 테이블 초안을 기준으로 한다.

검증:
- 문서 변경만 있는 경우 git diff로 변경 범위를 확인
```

---

## 3. Codex 작업 병렬화 기준

병렬로 맡겨도 되는 작업:

```text
Backend domain/test 작업
Frontend route skeleton 작업
문서 업데이트 작업
디자인 token 작업
```

병렬 금지 작업:

```text
같은 DB migration을 만지는 작업
같은 API contract를 동시에 바꾸는 작업
같은 feature page를 동시에 수정하는 작업
외부 API client와 snapshot 저장 로직을 동시에 대규모 변경하는 작업
```

---

## 4. 디자인 대개편 전까지의 UI 작업 기준

기능 개발 중에는 다음까지만 한다.

```text
읽을 수 있는 화면
기능 흐름 확인 가능
상태값 구분 가능
테이블/카드 기본 표시
반응형 깨짐 최소화
```

하지 않는다.

```text
브랜드 컬러 최종 확정
대시보드 시각 디자인 완성
복잡한 애니메이션
화려한 chart
전체 IA 재구성
```

---

## 5. 매 PR 리뷰 질문

Codex PR이 올라오면 아래를 확인한다.

```text
1. 이번 PR이 하나의 task만 해결했는가?
2. 테스트와 build가 통과했는가?
3. 외부 API 직접 호출 테스트가 없는가?
4. API key나 secret이 들어가지 않았는가?
5. 추천/수익 보장 문구가 없는가?
6. UI가 나중 디자인 대개편과 충돌하지 않는 구조인가?
7. 변경된 API/DB가 문서와 일치하는가?
```


---

## 로드맵 순서 보정

```text
P12 디자인 대개편
P13 앱인토스 Lite
```

앱인토스 Lite는 P12에서 확정한 디자인 시스템과 공통 컴포넌트를 재사용한다.
