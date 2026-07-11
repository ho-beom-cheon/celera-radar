# Seller Radar

셀러레이더는 셀러가 상품 후보를 검토할 때 쓰는 데이터 기반 상품 발굴 및 마진 분석 서비스입니다.

## 문서

- docs/01_screen_design.md: 화면설계서
- docs/02_interface_design.md: 인터페이스 설계서
- docs/03_system_design.md: 전체 설계서
- docs/04_codex_development_plan.md: Codex용 개발 진행 파일
- docs/05_refined_roadmap.md: 세분화 로드맵
- docs/07_codex_execution_plan_v2.md: Codex 개발 실행 계획 v2
- docs/09_decision_log.md: 주요 의사결정 로그
- docs/10_database_design.md: DB 상세 설계 기준 문서
- docs/11_progress_summary.md: 현재까지 작업 요약
- docs/13_external_api_credentials.md: 외부 API credential 준비 가이드
- AGENTS.md: Codex 작업 규칙 파일

## 로컬 실행

### IntelliJ 한 번에 실행

IntelliJ에서 아래 파일을 엽니다.

```text
backend/src/test/java/com/sellerradar/dev/RunLocalDev.java
```

이 파일을 연 상태에서 `Run Current File`을 실행하면 다음이 한 번에 시작됩니다.

- Docker PostgreSQL: `docker compose up -d db`
- Backend: `backend/gradlew.bat bootRun`
- Frontend: `frontend npm run dev -- --host 127.0.0.1`

실행 후 접속:

```text
http://127.0.0.1:5173/
```

테스트 계정:

```text
seller@example.com / password1234
```

중지할 때는 IntelliJ 실행 창에서 Stop을 누릅니다. 백엔드와 프론트는 함께 종료됩니다.
Docker DB는 계속 실행되며, 필요하면 프로젝트 루트에서 아래 명령으로 종료합니다.

```bash
docker compose down
```

### 1. PostgreSQL 실행

```bash
docker compose up -d db
```

또는 인프라 파일을 직접 지정합니다.

```bash
docker compose -f infra/docker-compose.yml up -d db
```

### 2. 백엔드 실행

Java 21이 필요합니다.
`JWT_SECRET`은 32바이트 이상 값으로 환경변수에 설정합니다.
네이버 신규 연동은 `NAVER_PROVIDER_MODE=HUB`와 API HUB 전용 credential을 사용합니다. production 기본값은 `DISABLED`입니다.
SmartStore 화면과 API는 mock 검증 기능이며 production에서는 backend와 frontend feature flag가 모두 기본 비활성화됩니다.
발급 경로와 적용 위치는 `docs/13_external_api_credentials.md`를 먼저 확인합니다.
운영 배포 전에는 `docs/16_production_configuration.md`의 prod profile과 secret fail-fast 기준을 확인합니다.

```bash
cd backend
./gradlew bootRun
```

로컬에서는 프로젝트 루트의 `.env`를 선택적으로 자동 로드합니다. `.env.example`을 `.env`로 복사한 뒤 실제 API HUB credential과 로컬 JWT secret을 입력합니다. `.env`는 Git에서 제외됩니다.

```powershell
Copy-Item .env.example .env
cd backend
.\gradlew.bat bootRun
```

### 3. 프론트엔드 실행

Node.js 20.19 이상이 필요합니다.

```bash
cd frontend
npm install
npm run dev
```

## 검증

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

## 규칙

- Secret, API key, JWT secret, 비밀번호를 커밋하지 않습니다.
- 테스트에서 외부 API를 직접 호출하지 않습니다.
- 도메인 로직, API 계약, DB 스키마, 점수 산식 변경 전 docs를 확인합니다.
- MVP에는 AI 호출, SmartStore 자동 상품 등록, 직접 도매 API 연동을 넣지 않습니다.
