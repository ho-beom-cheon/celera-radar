# 셀러레이더 Codex용 개발 진행 파일

- 문서 버전: v0.1
- 작성일: 2026-07-02
- 목적: Codex Pro가 작은 PR 단위로 구현할 수 있도록 작업 순서, 프롬프트, 검증 기준을 정의한다.

---

## 1. Codex 개발 원칙

1. 한 번에 한 기능만 구현한다.
2. DB 변경 → 백엔드 API → 테스트 → 프론트 화면 순서로 진행한다.
3. 외부 API는 테스트에서 직접 호출하지 않는다.
4. 모든 외부 API Client는 mock 테스트를 작성한다.
5. API Secret, JWT, 비밀번호를 코드에 하드코딩하지 않는다.
6. AI 호출 기능은 MVP에 넣지 않는다.
7. 설계서에 없는 기능은 구현하지 않는다.
8. 각 작업은 PR 단위로 끝내고, 검증 명령어 결과를 남긴다.

---

## 2. 저장소 구조

```text
seller-radar
├─ backend/
│  ├─ src/main/java/com/sellerradar
│  ├─ src/test/java/com/sellerradar
│  └─ build.gradle
├─ frontend/
│  ├─ src/
│  ├─ package.json
│  └─ vite.config.ts
├─ docs/
│  ├─ 01_screen_design.md
│  ├─ 02_interface_design.md
│  ├─ 03_system_design.md
│  └─ 04_codex_development_plan.md
├─ infra/
│  └─ docker-compose.yml
├─ .github/
│  ├─ workflows/
│  └─ pull_request_template.md
├─ AGENTS.md
└─ README.md
```

---

## 3. 기본 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Java 21, Spring Boot, Gradle |
| Frontend | React, TypeScript, Vite |
| DB | PostgreSQL |
| Auth | Spring Security, JWT |
| Batch | Spring Scheduler |
| Infra | Docker Compose |
| CI | GitHub Actions |
| Test | JUnit 5, Mockito, React build/type check |

---

## 4. 공통 검증 명령어

Backend:

```bash
cd backend
./gradlew test
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

Infra:

```bash
docker compose up -d db
docker compose ps
```

전체 PR은 가능한 한 다음 기준을 만족해야 한다.

```text
backend test 성공
frontend build 성공
secret 하드코딩 없음
외부 API 직접 호출 테스트 없음
문서 변경 필요 시 docs 업데이트
```

---

## 5. 브랜치/PR 규칙

브랜치 이름:

```text
feature/001-project-setup
feature/002-keyword-crud
feature/003-naver-shopping-client
fix/xxx
chore/xxx
```

PR 제목:

```text
[Feature] Keyword CRUD API 구현
```

PR 본문 템플릿:

```md
## 작업 내용
- 

## 검증
- [ ] ./gradlew test
- [ ] npm run build
- [ ] docker compose up -d db

## 영향 범위
- Backend:
- Frontend:
- DB:

## 참고
- docs/...
```

---

## 6. 작업 로드맵

## Phase 0. 프로젝트 초기화

### Task 001. 프로젝트 구조 생성

Codex 프롬프트:

```text
프로젝트 초기 구조를 생성해줘.

요구사항:
- backend: Spring Boot, Java 21, Gradle 프로젝트
- frontend: React + TypeScript + Vite 프로젝트
- infra/docker-compose.yml로 PostgreSQL 실행
- docs 디렉터리 생성
- README.md에 로컬 실행 방법 작성
- AGENTS.md 규칙을 준수

검증:
- backend ./gradlew test 성공
- frontend npm run build 성공
- docker compose up -d db 성공
```

완료 기준:

- 루트에서 backend/frontend/infra/docs 확인
- DB 컨테이너 실행 가능
- README에 로컬 실행 순서 존재

---

### Task 002. 공통 응답/에러 구조 구현

```text
backend에 공통 API 응답 구조를 구현해줘.

요구사항:
- ApiResponse<T>
- ApiError
- GlobalExceptionHandler
- requestId 생성 필터
- 표준 에러 코드 enum
- Validation 오류 응답 통일

검증:
- 단위 테스트 작성
- ./gradlew test 성공
```

---

## Phase 1. 인증/사용자/플랜

### Task 003. 사용자/플랜 기본 모델 구현

```text
users, subscription_plan, user_subscription 기본 모델을 구현해줘.

요구사항:
- User entity
- Plan enum: FREE, BASIC, PRO
- UserRepository
- 비밀번호 해시를 위한 PasswordEncoder 구성
- 초기 플랜은 FREE

검증:
- Repository 테스트
- ./gradlew test 성공
```

### Task 004. JWT 로그인/회원가입 구현

```text
회원가입/로그인 API를 구현해줘.

엔드포인트:
- POST /api/v1/auth/signup
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh

요구사항:
- 이메일 중복 방지
- 비밀번호 8자 이상 검증
- JWT Access/Refresh 토큰 발급
- Spring Security 적용

검증:
- Controller 테스트 또는 통합 테스트
- ./gradlew test 성공
```

---

## Phase 2. 키워드 레이더 v0.1

### Task 005. 카테고리/위험 카테고리 모델 구현

```text
category_master와 risk_category_rule 모델을 구현해줘.

요구사항:
- CategoryCode enum
- 초기 카테고리 seed 데이터
  - CAR_ACCESSORY
  - DESK_OFFICE
  - HOME_STORAGE
  - BATH_CLEANING
  - TRAVEL_ORGANIZER
  - PET_WALK_HYGIENE
  - KITCHEN_STORAGE
  - CAMPING_PICNIC
  - HOME_TRAINING
  - SEASONAL_LIVING
- 위험 카테고리 룰 seed
- 추천 제외/주의 여부 조회 서비스

검증:
- seed 데이터 테스트
- ./gradlew test 성공
```

### Task 006. 키워드 CRUD API 구현

```text
키워드 관리 기능을 구현해줘.

엔드포인트:
- GET /api/v1/keywords
- POST /api/v1/keywords
- PUT /api/v1/keywords/{keywordId}
- DELETE /api/v1/keywords/{keywordId}

요구사항:
- keyword_master 테이블
- 사용자별 중복 키워드 방지
- FREE 플랜 키워드 3개 제한
- 삭제는 soft delete
- analysisStatus 기본 PENDING

검증:
- Service 단위 테스트
- Controller 통합 테스트
- ./gradlew test 성공
```

### Task 007. 키워드 목록 프론트 화면 구현

```text
frontend에 키워드 목록 화면을 구현해줘.

요구사항:
- /keywords 라우트
- 키워드 등록 폼
- 키워드 목록 테이블
- 상태 배지 표시
- 무료 플랜 한도 메시지
- API client 분리

검증:
- npm run build 성공
```

---

## Phase 3. 네이버 쇼핑 검색 연동

### Task 008. NaverShoppingClient 구현

```text
네이버 쇼핑 검색 API Client를 구현해줘.

요구사항:
- NaverShoppingClient
- application.yml에 client-id/client-secret 설정 키만 정의
- 실제 값은 환경변수로 주입
- query, display, start, sort, exclude 파라미터 지원
- 외부 API 응답 DTO 작성
- 테스트는 MockWebServer 또는 WireMock 사용
- 테스트에서 실제 네이버 API 호출 금지

검증:
- Client 테스트 작성
- ./gradlew test 성공
```

### Task 009. 쇼핑 검색 스냅샷 저장 구현

```text
네이버 쇼핑 검색 결과를 snapshot으로 저장하는 기능을 구현해줘.

요구사항:
- shopping_price_snapshot 테이블
- shopping_top_item 테이블
- keyword별 baseDate 기준 중복 저장 방지
- 오늘 이미 성공한 키워드는 외부 API 재호출하지 않음
- api_call_log 저장

검증:
- 캐시 사용 테스트
- 중복 방지 테스트
- ./gradlew test 성공
```

### Task 010. 쇼핑 검색 수동 배치 API 구현

```text
관리자용 쇼핑 검색 수동 배치 API를 구현해줘.

엔드포인트:
- POST /api/v1/admin/batches/shopping-search/run
- GET /api/v1/admin/batches

요구사항:
- active keyword만 대상
- batch_job_history 저장
- 실패 키워드 수 기록
- 관리자 권한 필요

검증:
- Service 테스트
- ./gradlew test 성공
```

### Task 011. 키워드 상세 분석 화면 v0.1

```text
frontend에 키워드 상세 화면을 구현해줘.

요구사항:
- /keywords/:keywordId
- 최저가/평균가/검색 결과 수 표시
- 상위 상품 테이블
- 캐시 기준일 표시
- 데이터가 없으면 분석 대기 상태 표시

검증:
- npm run build 성공
```

---

## Phase 4. 데이터랩 트렌드 v0.2

### Task 012. NaverDataLabClient 구현

```text
네이버 데이터랩 쇼핑인사이트 API Client를 구현해줘.

요구사항:
- category keyword trend endpoint 지원
- startDate, endDate, timeUnit, category, keyword 요청 지원
- 하루 호출 한도 관리를 위한 ApiQuotaService 설계
- 테스트는 MockWebServer/WireMock 사용

검증:
- Client 테스트
- quota 테스트
- ./gradlew test 성공
```

### Task 013. trend_snapshot 저장 및 점수 계산

```text
trend_snapshot 저장과 trend_score 계산을 구현해줘.

요구사항:
- trend_snapshot 테이블
- 최근 7일/30일 delta 계산
- trend_score 0~30 산출
- 데이터랩 ratio는 실제 판매량이 아니라는 warning reason 포함

검증:
- 점수 계산 단위 테스트
- ./gradlew test 성공
```

---

## Phase 5. 추천점수 v0.2~v0.3

### Task 014. ScoringEngine 구현

```text
추천점수 계산 엔진을 구현해줘.

요구사항:
- trendScore 0~30
- competitionScore 0~25
- marginScore 0~30
- priceBandScore 0~10
- supplyScore 0~5
- riskPenalty 0~-40
- overallScore 계산
- grade: RECOMMENDED/REVIEW/HOLD/EXCLUDED
- score reasons/warnings 저장

검증:
- 경계값 테스트
- 위험 카테고리 제외 테스트
- ./gradlew test 성공
```

### Task 015. 후보 목록/상세 API 구현

```text
product_candidate와 candidate_score API를 구현해줘.

엔드포인트:
- GET /api/v1/candidates
- GET /api/v1/candidates/{candidateId}
- POST /api/v1/candidates/{candidateId}/save
- POST /api/v1/candidates/{candidateId}/exclude

검증:
- 필터 테스트
- 저장/제외 처리 테스트
- ./gradlew test 성공
```

---

## Phase 6. 도매 CSV v0.3

### Task 016. CSV 업로드/파일 메타 구현

```text
도매 CSV 업로드 기능을 구현해줘.

엔드포인트:
- POST /api/v1/wholesale-files
- GET /api/v1/wholesale-files/{fileId}

요구사항:
- .csv만 MVP 지원
- UTF-8/CP949 인코딩 자동 감지 시도
- 파일 메타 저장
- 플랜별 행 제한 적용
- 원본 파일 경로는 외부 노출 금지

검증:
- 업로드 테스트
- 행 제한 테스트
- ./gradlew test 성공
```

### Task 017. CSV 컬럼 매핑/파싱 구현

```text
CSV 컬럼 매핑과 파싱 기능을 구현해줘.

엔드포인트:
- POST /api/v1/wholesale-files/{fileId}/column-mapping
- POST /api/v1/wholesale-files/{fileId}/parse
- GET /api/v1/wholesale-files/{fileId}/rows

요구사항:
- 필수 컬럼: productName, supplyPrice
- 선택 컬럼: shippingFee, category, productUrl
- 숫자 파싱 오류 행 저장
- wholesale_product 저장

검증:
- 정상 CSV 테스트
- 오류 CSV 테스트
- ./gradlew test 성공
```

### Task 018-A. CSV 마진 계산 모듈 구현

```text
CSV 상품의 예상 판매가와 예상 마진을 계산하는 순수 모듈을 구현해줘.

요구사항:
- expectedSalePrice 산출
- supplyPrice/shippingFee 기반 margin 계산

검증:
- 마진 계산 테스트
- ./gradlew test 성공
```

### Task 018-B. CSV 상품 매칭 로직 구현

```text
CSV 상품과 기존 키워드/쇼핑 스냅샷을 연결하는 매칭 로직을 구현해줘.

요구사항:
- 상품명 정규화
- 연결 가능한 keyword 또는 shopping snapshot 매칭
- 매칭 실패 시에도 후보 생성 가능하도록 fallback 가격 정책 분리

검증:
- 상품명 정규화 테스트
- 키워드/스냅샷 매칭 테스트
- ./gradlew test 성공
```

### Task 018-C. CSV 후보 생성 API 구현

```text
CSV 상품을 product_candidate로 변환하는 API를 구현해줘.

요구사항:
- product_candidate 생성
- candidate_score 생성
- 동일 wholesale_product 중복 후보 생성 방지
- 후보 생성 결과 개수 반환

검증:
- 후보 생성 테스트
- ./gradlew test 성공
```

---

## Phase 7. 알림 v0.4

### Task 019. 알림 조건/알림 생성 구현

```text
알림 조건과 알림 생성 기능을 구현해줘.

엔드포인트:
- GET /api/v1/alerts
- POST /api/v1/alert-rules
- PATCH /api/v1/alerts/{alertId}/read

요구사항:
- minScore, minMarginRate, categoryCodes, riskExcluded, frequency
- 중복 알림 방지
- ALERT_GENERATE_DAILY 배치 서비스

검증:
- 조건 매칭 테스트
- 중복 알림 방지 테스트
- ./gradlew test 성공
```

### Task 020. 알림 프론트 화면 구현

```text
frontend에 알림 목록과 알림 조건 설정 화면을 구현해줘.

요구사항:
- /alerts
- /alert-rules
- 읽음 처리
- 추천 후보 상세 이동

검증:
- npm run build 성공
```

---

## Phase 8. 앱인토스 Lite v1.0 - 보류

현재 결정:
- Web SaaS 앱을 먼저 다듬고 출시 준비가 끝난 뒤 재개한다.
- `다음` 요청으로는 Task 021~022를 진행하지 않는다.
- 앱인토스 SDK, Granite 설정, `/toss` 라우트는 재개 전까지 추가하지 않는다.

### Task 021. 앱인토스 Lite 라우트 추가 - 보류

```text
frontend에 앱인토스 Lite용 라우트를 추가해줘.

요구사항:
- /toss
- /toss/margin
- /toss/candidates/:candidateId
- /toss/keywords
- 모바일 360px 기준 카드 UI
- 외부 웹 SaaS 이동을 핵심 플로우로 두지 않음

검증:
- npm run build 성공
```

### Task 022. 앱인토스 SDK 초기 설정 - 보류

```text
앱인토스 WebView SDK 초기 설정 파일을 추가해줘.

요구사항:
- @apps-in-toss/web-framework 설치 전제
- granite.config.ts 추가
- appName/displayName/icon placeholder
- TDS 패키지 적용은 TODO로 남김
- 실제 토스 콘솔 정보는 환경별 설정으로 분리

검증:
- npm run build 성공
```

---

## 7. Codex에게 주면 안 되는 큰 요청

다음 식의 요청은 금지한다.

```text
셀러레이더 전체를 한 번에 만들어줘.
상품 추천 앱을 완성해줘.
네이버 API랑 앱인토스랑 결제랑 스마트스토어까지 다 붙여줘.
```

반드시 다음처럼 작은 단위로 요청한다.

```text
keyword_master 테이블과 CRUD API만 구현해줘.
NaverShoppingClient만 구현하고 테스트는 MockWebServer로 작성해줘.
trend_score 계산 함수와 경계값 테스트만 작성해줘.
```

---

## 8. 환경변수 초안

Backend:

```text
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://localhost:5432/seller_radar
DB_USERNAME=seller
DB_PASSWORD=seller
JWT_SECRET=change-me
NAVER_CLIENT_ID=
NAVER_CLIENT_SECRET=
```

Frontend:

```text
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_ENV=local
```

앱인토스 (보류, 재개 시에만 사용):

```text
AIT_APP_NAME=seller-radar-lite
AIT_DISPLAY_NAME=셀러레이더
```

---

## 9. 초기 README에 포함할 내용

```md
# Seller Radar

## Local Run
1. docker compose up -d db
2. cd backend && ./gradlew bootRun
3. cd frontend && npm install && npm run dev

## Test
- backend: ./gradlew test
- frontend: npm run build

## Rules
- Do not commit secrets.
- Do not call external APIs in tests.
- Read docs before changing domain logic.
```

---

## 10. 최종 개발 순서 요약

```text
001 프로젝트 구조
002 공통 응답/에러
003 사용자/플랜
004 JWT 인증
005 카테고리/위험룰
006 키워드 CRUD
007 키워드 프론트
008 네이버 쇼핑 Client
009 쇼핑 Snapshot 저장
010 쇼핑 배치
011 키워드 상세 화면
012 데이터랩 Client
013 Trend Snapshot/Score
014 Scoring Engine
015 Candidate API
016 CSV 업로드
017 CSV 파싱
018-A CSV 마진 계산
018-B CSV 상품 매칭
018-C CSV 후보 생성
019 Alert API
020 Alert 화면
021 앱인토스 Lite 화면 (보류)
022 앱인토스 SDK 초기 설정 (보류)
```
