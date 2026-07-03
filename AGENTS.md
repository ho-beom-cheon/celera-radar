# AGENTS.md

## Project
Seller Radar is a seller product discovery and margin analysis service.

The first product is a Web SaaS dashboard. Apps in Toss Lite is a later lightweight entry channel. A native mobile app is not part of the MVP.

## Tech Stack
- Backend: Java 21, Spring Boot, Gradle
- Frontend: React, TypeScript, Vite
- DB: PostgreSQL
- Batch: Spring Scheduler
- Infra: Docker Compose
- Test: JUnit 5, Mockito, MockWebServer or WireMock

## Product Rules
- The service provides data-based product candidates for review, not guaranteed best-selling products.
- Do not use phrases that imply guaranteed sales or guaranteed profit.
- Prefer terms such as "검토 후보", "추천 검토 후보", "데이터 기반 후보".
- MVP must not include AI calls.
- MVP must not include SmartStore automatic product registration.
- MVP must not include direct wholesale API integration; use CSV upload first.

## API Rules
- Do not call Naver APIs directly from frontend.
- Do not call external APIs in tests.
- Use MockWebServer or WireMock for external API client tests.
- Cache all external API results in PostgreSQL snapshots.
- Do not call external APIs when a successful snapshot already exists for the same keyword and base date.
- Store API call results in api_call_log.
- Do not hardcode API keys or secrets.

## Security Rules
- Never commit secrets, API keys, JWT secrets, passwords, or real user data.
- Do not log Authorization headers, cookies, API secrets, or raw passwords.
- Uploaded CSV files must not be publicly exposed.
- Use environment variables for credentials.

## Coding Rules
- Keep changes small and reviewable.
- Implement one task per PR.
- Do not implement unrelated features.
- Add or update tests for backend business logic.
- Update docs when changing API contract, DB schema, or scoring logic.
- Use clear package boundaries: auth, user, keyword, shopping, trend, wholesale, margin, candidate, scoring, alert, batch, common.

## Verification
Before completing a task, run the relevant commands.

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

## External API Notes
- Naver Shopping Search API is used for shopping result snapshots and price range analysis.
- Naver DataLab Shopping Insight API is used for search click trend ratio, not actual sales volume.
- Apps in Toss Lite must complete its core mini-app functions inside Toss and must not depend on external site navigation for the main flow.

## Done Criteria
- Tests pass.
- Build passes.
- No secret is committed.
- External API calls are mocked in tests.
- Any new endpoint is documented or aligned with docs/02_interface_design.md.
- Any scoring logic change is documented or aligned with docs/03_system_design.md.
