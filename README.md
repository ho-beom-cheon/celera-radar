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
네이버 API 연동 기능을 실행할 때는 `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`을 환경변수로 설정합니다.

```bash
cd backend
./gradlew bootRun
```

Windows PowerShell:

```powershell
cd backend
$env:JWT_SECRET="replace-with-at-least-32-byte-local-secret"
$env:NAVER_CLIENT_ID=""
$env:NAVER_CLIENT_SECRET=""
$env:NAVER_DATALAB_DAILY_QUOTA="1000"
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
