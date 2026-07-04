# 11. 현재까지 작업 요약

- 기준일: 2026-07-03
- 프로젝트: Seller Radar
- 현재 브랜치: `main`

---

## 1. 프로젝트 기반 구축

셀러레이더 MVP 기준의 기본 저장소 구조를 구성했다.

반영된 주요 영역:

- `backend`: Java 21, Spring Boot, Gradle 기반 백엔드
- `frontend`: React, TypeScript, Vite 기반 프론트엔드
- `docker-compose.yml`: PostgreSQL 로컬 실행 구성
- `infra/docker-compose.yml`: 인프라 실행용 동일 DB 구성
- `.env.example`: 로컬 환경변수 예시
- `.github`: 이슈/PR 및 워크플로우 기반 파일
- `AGENTS.md`: Codex 작업 규칙

현재 Docker DB:

```text
container: seller-radar-db
database: seller_radar
user: seller
password: seller
port: 5432
```

---

## 2. 설계 문서 반영

초기 설계 문서와 DB 상세 설계 문서를 저장소에 반영했다.

주요 문서:

- `docs/01_screen_design.md`
- `docs/02_interface_design.md`
- `docs/03_system_design.md`
- `docs/04_codex_development_plan.md`
- `docs/05_refined_roadmap.md`
- `docs/07_codex_execution_plan_v2.md`
- `docs/09_decision_log.md`
- `docs/10_database_design.md`
- `docx/10_database_design.docx`

`docs/10_database_design.md`를 DB migration과 domain 구현의 기준 문서로 정했다.

Phase 순서는 아래 기준으로 정리했다.

```text
P0 기반 구축
P1 키워드 레이더
P2 네이버 쇼핑 스냅샷
P3 가격/경쟁 분석
P4 데이터랩 트렌드 분석
P5 도매 CSV/XLSX 업로드
P6 마진/상품 검토 점수
P7 알림/배치
P8 Web SaaS 하드닝
P9 스마트스토어 API 1차 연동
P10 스마트스토어 내 상품 마진 감시
P11 주문/정산/수수료 연동
P12 디자인 대개편
P13 앱인토스 Lite
P14 베타/수익화
```

앱인토스 Lite는 기능과 디자인을 다듬은 뒤 진행하는 것으로 보류했다.

---

## 3. 로컬 실행 환경

Docker Desktop과 PostgreSQL 실행을 확인했다.

검증된 실행 상태:

- PostgreSQL: `seller-radar-db` healthy
- Backend: `http://127.0.0.1:8080/actuator/health`
- Frontend: `http://127.0.0.1:5173/`

pgAdmin 연결 정보:

```text
Name: seller-radar
Host name/address: 127.0.0.1
Port: 5432
Maintenance database: seller_radar
Username: seller
Password: seller
```

pgAdmin 경로:

```text
Servers > seller-radar > Databases > seller_radar > Schemas > public > Tables
```

---

## 4. IntelliJ 실행 설정

IntelliJ에서 한 번에 로컬 개발 서버를 실행할 수 있도록 설정했다.

추가한 실행 파일:

- `backend/src/test/java/com/sellerradar/dev/RunLocalDev.java`

추가한 IntelliJ 실행 설정:

- `.idea/runConfigurations/Seller_Radar_Local_Dev.xml`
- 실행 이름: `Seller Radar Local Dev`

동작:

```text
Docker PostgreSQL 실행
Backend bootRun 실행
Frontend Vite dev server 실행
기존 8080/5173 프로세스가 있으면 재사용
```

---

## 5. 구현된 주요 기능

현재까지 구현된 주요 기능 영역은 다음과 같다.

- 인증/회원가입/로그인
- JWT 기반 인증 필터
- 키워드 등록/조회/수정/삭제 API 골격
- 네이버 쇼핑 검색 client와 snapshot 저장 흐름
- 데이터랩 트렌드 client와 snapshot 저장 흐름
- 도매 CSV 업로드/파싱/후보 생성 흐름
- 마진 계산
- 상품 검토 후보와 점수 계산
- 알림 rule/event 및 batch skeleton
- 카테고리 seed/risk rule
- 프론트 키워드/후보/도매/알림 관련 기본 화면

MVP 규칙에 따라 다음은 아직 넣지 않았다.

- AI 호출
- SmartStore 자동 상품 등록
- 직접 도매 API 연동
- Apps in Toss Lite 구현

---

## 6. P1-001 구현 내용

`docs/10_database_design.md` 기준으로 `P1-001 users/keywords migration 및 domain 구현`을 진행했다.

### 6.1 Flyway migration

추가 파일:

- `backend/src/main/resources/db/migration/V001__create_users.sql`
- `backend/src/main/resources/db/migration/V002__create_keywords.sql`

적용 내용:

- `users` 테이블 기준 컬럼 정리
  - `public_id`
  - `email`
  - `display_name`
  - `plan_code`
  - `active`
  - `created_at`
  - `updated_at`
  - `deleted_at`
- `keywords` 테이블 기준 컬럼 정리
  - `user_id`
  - `keyword`
  - `normalized_keyword`
  - `category`
  - `active`
  - `analysis_status`
  - `last_analyzed_at`
  - `last_snapshot_date`
  - `created_at`
  - `updated_at`
  - `deleted_at`
- `keyword_master` 기존 데이터가 있으면 `keywords`로 이관
- 기존 `ANALYZED` 상태는 `SUCCESS`로 변환

Flyway 적용 확인:

```text
0   << Flyway Baseline >> true
001 create users          true
002 create keywords       true
```

### 6.2 Flyway 실행 설정

Spring Boot 4 환경에서 Flyway 자동설정이 기대대로 동작하지 않아 명시 설정을 추가했다.

추가 파일:

- `backend/src/main/java/com/sellerradar/common/db/FlywayMigrationConfig.java`

동작:

- `spring.flyway.enabled=true`일 때 Flyway migration 실행
- JPA `entityManagerFactory`가 Flyway migration 이후 초기화되도록 `dependsOn` 적용
- 테스트 환경에서는 `spring.flyway.enabled=false`

### 6.3 User domain

수정 파일:

- `backend/src/main/java/com/sellerradar/user/domain/User.java`
- `backend/src/main/java/com/sellerradar/user/repository/UserRepository.java`

반영 내용:

- `publicId` 추가
- `displayName` 추가
- `active` 추가
- `deletedAt` 추가
- soft delete 메서드 추가
- active user 기준 email 조회 메서드 추가

### 6.4 Keyword domain

수정 파일:

- `backend/src/main/java/com/sellerradar/keyword/domain/Keyword.java`
- `backend/src/main/java/com/sellerradar/keyword/domain/AnalysisStatus.java`
- `backend/src/main/java/com/sellerradar/keyword/repository/KeywordRepository.java`

반영 내용:

- 테이블명 `keyword_master`에서 `keywords` 기준으로 변경
- `active/deleted_at` 기반 soft delete 구조 적용
- `category` 문자열 컬럼 기준으로 변경
- `last_snapshot_date` 추가
- 분석 상태를 설계 기준으로 변경

분석 상태:

```text
PENDING
RUNNING
SUCCESS
FAILED
SKIPPED
```

기존 API/배치 코드가 깨지지 않도록 아래 호환 메서드는 유지했다.

- `getStatus()`
- `getCategoryCode()`
- `getPriority()`
- `findByStatus(...)`
- `findByUserIdAndStatus(...)`

### 6.5 Frontend 상태값 정합성

백엔드 분석 상태가 `ANALYZED`에서 `SUCCESS`로 바뀌면서 프론트 타입과 라벨을 최소 수정했다.

수정 파일:

- `frontend/src/api/keywords.ts`
- `frontend/src/routes/keywords/KeywordsPage.tsx`

반영 상태:

```text
PENDING: 대기
RUNNING: 분석 중
SUCCESS: 완료
FAILED: 실패
SKIPPED: 건너뜀
```

---

## 7. 검증 결과

Backend test:

```bash
cd backend
./gradlew test
```

결과:

```text
BUILD SUCCESSFUL
```

Frontend build:

```bash
cd frontend
tsc
vite build
```

결과:

```text
✓ built
```

Runtime:

```text
Backend  http://127.0.0.1:8080/actuator/health  200
Frontend http://127.0.0.1:5173/                 200
DB       seller-radar-db                         healthy
```

DB 확인:

```text
users table exists
keywords table exists
flyway_schema_history exists
```

현재 데이터:

```text
users: 1
keywords: 2
```

---

## 8. P1-002 구현 완료

`docs/10_database_design.md` 기준으로 Keyword REST API 정합성 정리를 완료했다.

반영 내용:

- keyword create/list/detail/update/delete 요청/응답 DTO 정리
- list 기본 조회 조건을 `active=true`, `deleted_at is null` 기준으로 정리
- `analysisStatus` 필터를 `PENDING/RUNNING/SUCCESS/FAILED/SKIPPED` 기준으로 제한
- category는 문자열 필드로 유지
- 동일 사용자 내 `normalizedKeyword` 중복 등록 방지
- delete는 physical delete가 아니라 soft delete로 처리
- frontend keyword API 타입을 backend 응답과 정합화
- `docs/02_interface_design.md` 최소 갱신

검증:

```text
backend test 성공
frontend build 성공
docker compose DB 상태에서 keyword 등록/조회/수정/삭제 확인
```

---

## 9. P2-001 구현 완료

`docs/10_database_design.md` 기준으로 shopping snapshot migration과 domain/repository 정합성 정리를 완료했다.

반영 내용:

- `V003__create_shopping_snapshots.sql` 추가
- `shopping_search_snapshots` 테이블 추가
- `shopping_item_snapshots` 테이블 추가
- `api_call_logs` 테이블 추가
- 기존 shopping snapshot domain을 설계서의 `search_date`, `sort_type`, `competition_level`, `status` 기준으로 정리
- item snapshot domain을 `rank_no`, `title_raw`, `product_url`, `image_url`, `low_price`, `high_price` 기준으로 정리
- API call log domain을 `provider`, `endpoint`, `request_key`, `success`, `called_at` 기준으로 정리
- shopping/trend API log provider를 `NAVER_SEARCH`, `NAVER_DATALAB`로 분리
- repository와 관련 테스트 보강

검증:

```text
cd backend && ./gradlew.bat test
BUILD SUCCESSFUL

docker compose up -d db
seller-radar-db running

backend bootRun
Flyway v003 적용 성공
```

DB 확인:

```text
flyway_schema_history: 003 create shopping snapshots success
tables: shopping_search_snapshots, shopping_item_snapshots, api_call_logs
indexes: uk_shopping_snapshot_keyword_date_sort, uk_shopping_item_snapshot_rank, idx_api_call_logs_request_key
```

진행율:

```text
docs/10_database_design.md 14.1 기준 4/7 완료
완료: P0-001, P1-001, P1-002, P2-001
다음: P2-002
```

---

## 10. P2-002 구현 완료

`docs/10_database_design.md` 기준으로 `P2-002 NaverShoppingClient mock test` 정합성 정리를 완료했다.

반영 내용:

- Naver Shopping Search client 테스트 fixture를 읽기 쉬운 ASCII 샘플로 정리
- MockWebServer 기반 정상 응답 DTO 매핑 검증
- 요청 header 검증
- `query`, `display`, `start`, `sort`, `exclude` query parameter 검증
- `sort` 기본값 `sim` 검증
- 빈 `exclude`는 요청에서 생략되는지 검증
- 429 응답을 `EXTERNAL_API_RATE_LIMIT`로 매핑하는지 검증
- 400/500 응답을 `EXTERNAL_API_UNAVAILABLE`로 매핑하는지 검증
- credential 누락 시 외부 요청 전 실패하는지 검증

검증:

```text
cd backend && ./gradlew.bat test
BUILD SUCCESSFUL
```

진행율:

```text
docs/10_database_design.md 14.1 기준 5/7 완료
완료: P0-001, P1-001, P1-002, P2-001, P2-002
다음: P2-003
```

---

## 11. P2-003 구현 완료

`docs/10_database_design.md` 기준으로 `P2-003 분석 API 및 snapshot 저장` 정합성 정리를 완료했다.

반영 내용:

- `POST /api/v1/keywords/{keywordId}/analyze/shopping` 추가
- `GET /api/v1/keywords/{keywordId}/shopping-snapshot/latest` 추가
- 분석 실행 응답에 `cached`, `searchDate`, `sortType`, `totalCount`, `minPrice`, `maxPrice`, `avgPrice`, `fetchedAt` 포함
- 상위 item 응답에 `rankNo`, `title`, `productUrl`, `imageUrl`, `lowPrice`, `mallName`, `category1~4` 포함
- 같은 `keyword_id + search_date + sort_type` snapshot이 있으면 외부 API를 재호출하지 않고 캐시 응답 반환
- 분석 성공 시 keyword `analysisStatus`, `lastAnalyzedAt`, `lastSnapshotDate` 갱신
- 분석 실패 시 keyword `analysisStatus=FAILED` 및 `api_call_logs` 실패 이력 저장 유지
- snapshot 미존재 시 최신 snapshot 조회 API는 `ANALYSIS_NOT_READY` 반환
- controller integration test와 service test 보강
- `docs/02_interface_design.md` 최소 갱신

검증:

```text
cd backend && ./gradlew.bat test
BUILD SUCCESSFUL
```

진행율:

```text
docs/10_database_design.md 14.1 기준 6/7 완료
완료: P0-001, P1-001, P1-002, P2-001, P2-002, P2-003
다음: P2-004
```

---

## 12. 다음 작업 제안

다음 구현 작업은 `P2-004 상품 카드 화면`이 적절하다.

작업 범위:

- frontend keyword 상세 화면에서 분석 실행 버튼 추가
- 최신 shopping snapshot 조회 및 가격 요약 표시
- 상위 상품 카드 10개 표시
- imageUrl 누락/로드 실패 placeholder 처리
- frontend keyword API 타입을 P2-003 backend 응답과 정합화
- `npm run build`와 브라우저 화면 확인
## 최신 진행: P2-004 구현 완료

`docs/10_database_design.md`와 `docs/07_codex_execution_plan_v2.md` 기준으로 `P2-004 상품 카드 화면` 구현을 완료했다.

반영 내용:

- 키워드 상세 화면에서 `GET /api/v1/keywords/{keywordId}/shopping-snapshot/latest`를 조회하도록 정리
- 키워드 상세 화면에 `POST /api/v1/keywords/{keywordId}/analyze/shopping` 분석 실행 버튼 추가
- 최신 shopping snapshot의 `totalCount`, `minPrice`, `avgPrice`, `maxPrice`, `searchDate`, `fetchedAt` 표시
- 상위 상품 10개를 `ProductCard` 컴포넌트로 분리해 카드 형태로 표시
- 상품 카드에 이미지, 상품명, 최저가, 몰명, 카테고리, 원본 링크 표시
- 이미지 URL이 없거나 로드 실패 시 placeholder 표시
- frontend keyword API 타입을 P2-003 backend 응답 필드와 정합화

검증:

```text
cd frontend && npm run build
BUILD SUCCESSFUL
```

진행률:

```text
docs/10_database_design.md 14.1 기준 7/7 완료
완료: P0-001, P1-001, P1-002, P2-001, P2-002, P2-003, P2-004
다음: P3-001
```

다음 구현 작업은 `P3-001 Competition Analyzer 구현`이 적절하다.

---
## 최신 진행: P3-002 구현 완료

`docs/10_database_design.md`와 `docs/07_codex_execution_plan_v2.md` 기준으로 `P3-002 가격/경쟁강도 화면 표시`를 완료했다.

반영 내용:

- Keyword list/detail 응답에 최신 쇼핑 스냅샷 요약 필드 추가
  - `latestMinPrice`
  - `latestAvgPrice`
  - `latestCompetitionLevel`
- 쇼핑 스냅샷 조회 응답에 `competitionLevel` 추가
- 키워드 목록에 최신 최저가, 평균가, 경쟁강도, 마지막 분석 시각 표시
- 키워드 상세 가격 요약에 경쟁강도 badge 표시
- frontend keyword API 타입을 backend 응답과 정합화
- `docs/02_interface_design.md`에 변경된 응답 필드 반영

검증:

```text
cd backend && ./gradlew.bat test
BUILD SUCCESSFUL

cd frontend && npm.cmd run build
BUILD SUCCESSFUL
```

진행률:

```text
P3 기준 2/2 완료
완료: P3-001, P3-002
다음: P4-001
```

다음 구현 작업은 `P4-001 Trend Snapshot DB/Domain 구현`이 적절하다.

---

## 최신 진행: P3-001 구현 완료

`docs/10_database_design.md`와 `docs/07_codex_execution_plan_v2.md` 기준으로 `P3-001 Competition Analyzer 구현`을 완료했다.

반영 내용:

- `CompetitionAnalyzer` 추가
- 검색 결과 수 기준 경쟁 강도 계산 적용
  - `totalCount >= 10000`: `HIGH`
  - `totalCount >= 3000`: `MEDIUM`
  - 그 외: `LOW`
- totalCount가 없거나 저장된 item이 없으면 `UNKNOWN`으로 유지
- DB 호환을 위해 enum의 `UNKNOWN`, `VERY_HIGH` 값은 유지하되 P3-001 계산 결과는 `LOW/MEDIUM/HIGH`로 제한
- shopping snapshot 생성 시 계산된 `competitionLevel`을 명시적으로 저장
- 가격 평균/중앙값 계산에서 0원 또는 빈 가격을 제외하는 테스트 보강

검증:

```text
cd backend && ./gradlew.bat test
BUILD SUCCESSFUL
```

진행률:

```text
P3 기준 1/2 완료
완료: P3-001
다음: P3-002
```

다음 구현 작업은 `P3-002 가격/경쟁강도 화면 표시`가 적절하다.

---

## 최신 진행: P4-001 구현 완료

`docs/10_database_design.md`와 `docs/07_codex_execution_plan_v2.md` 기준으로 `P4-001 Trend Snapshot DB/Domain 구현`을 완료했다.

반영 내용:

- `trend_snapshots` Flyway migration 추가
- `batch_job_history` Flyway migration 추가
- 기존 `TrendSnapshot` domain을 `trend_snapshots` 테이블과 snapshot/filter 컬럼 구조에 맞게 보정
- trend snapshot 저장/조회 repository 메서드를 `snapshotDate`, `dataPeriod`, `timeUnit` 기준으로 정리
- TrendSnapshot service/test 정합성 보강
- PostgreSQL 임시 DB에서 V001~V004 migration 적용 확인

검증:

```text
cd backend && .\gradlew.bat test
BUILD SUCCESSFUL

PostgreSQL temporary migration check
V001~V004 applied successfully
```

진행률:

```text
P4 데이터랩 트렌드 분석: 1/3 완료
- P4-001 Trend Snapshot DB/Domain 구현: 완료
- P4-002 NaverDataLabClient 구현: 다음
- P4-003 Daily Trend Batch 구현: 대기
```

다음: P4-002

---
## 최신 진행: P4-002 구현 완료

`docs/07_codex_execution_plan_v2.md` 기준으로 `P4-002 NaverDataLabClient 구현` 정합성 보강을 완료했다.

반영 내용:

- 기존 단일 키워드 DataLab 요청 호환 유지
- 한 요청에 최대 5개 `keywordGroups`를 보낼 수 있도록 요청 모델 보강
- DataLab 요청 payload가 `keywordGroups`를 공식 `keyword` 배열로 전송하도록 정리
- 5개 초과 keyword group validation 추가
- MockWebServer client test 보강
- 429 rate limit 응답과 500 server error 응답 매핑 확인

검증:

```text
cd backend && .\gradlew.bat test
BUILD SUCCESSFUL
```

진행률:

```text
P4 데이터랩 트렌드 분석: 2/3 완료
- P4-001 Trend Snapshot DB/Domain 구현: 완료
- P4-002 NaverDataLabClient 구현: 완료
- P4-003 Daily Trend Batch 구현: 다음
```

다음: P4-003

---
## 최신 진행: P4-003 구현 완료

`docs/07_codex_execution_plan_v2.md` 기준으로 `P4-003 Daily Trend Batch 구현`을 완료했다.

반영 내용:

- `DATALAB_TREND_DAILY` batch job type 추가
- DataLab trend daily target limit 설정 추가: `DATALAB_DAILY_TARGET_LIMIT`
- ACTIVE keyword 대상 DailyTrendBatchService 추가
- keyword별 실패를 전체 batch 실패로 전파하지 않고 failureCount로 기록
- `/api/v1/admin/batches/datalab/run` 관리자 수동 실행 endpoint 추가
- service/controller 테스트 보강
- PostgreSQL 임시 DB에서 V001~V004 migration 적용 확인

검증:

```text
cd backend && .\gradlew.bat test
BUILD SUCCESSFUL

PostgreSQL temporary migration check
V001~V004 applied successfully
```

진행률:

```text
P4 데이터랩 트렌드 분석: 3/3 완료
- P4-001 Trend Snapshot DB/Domain 구현: 완료
- P4-002 NaverDataLabClient 구현: 완료
- P4-003 Daily Trend Batch 구현: 완료
```

다음: P5-001

---
