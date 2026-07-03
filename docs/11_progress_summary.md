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

## 8. 현재 주의사항

현재 저장소는 초기 구축 과정 때문에 untracked 파일이 많다.

대표 상태:

```text
A  AGENTS.md
M  README.md
?? backend/
?? frontend/
?? docs/
?? docx/
?? docker-compose.yml
?? infra/
?? .github/
?? .env.example
?? .gitignore
```

`backend/`와 `frontend/`가 아직 전체 untracked 상태라 `git diff --stat`에는 실제 구현 변경이 충분히 드러나지 않는다.

커밋 전에는 다음 단위로 나눠 정리하는 것이 좋다.

```text
1. 기반 구조/실행 설정
2. 설계 문서 반영
3. P1-001 DB/domain 구현
4. 프론트 상태값 정합성 보정
```

---

## 9. 다음 작업 제안

다음 구현 작업은 `P1-002 Keyword REST API 정합성 정리`가 적절하다.

작업 범위:

- `docs/10_database_design.md` 기준으로 Keyword API 요청/응답 필드 정리
- 분석 상태 필터를 `PENDING/RUNNING/SUCCESS/FAILED/SKIPPED` 기준으로 정리
- category 필드와 기존 `CategoryCode` 사용 방식 결정
- keyword list/create/update/delete 통합 테스트 보강
- API 문서가 필요하면 `docs/02_interface_design.md` 최소 갱신

실제 구현은 다음 작업에서 진행한다.
