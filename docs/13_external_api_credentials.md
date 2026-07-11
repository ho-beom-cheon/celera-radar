# 13. External API credential preparation guide

- 기준일: 2026-07-12
- 범위: 실제 외부 API 연동 전 credential 발급, 보관, 로컬 적용 위치 정리
- 제외: 실제 API 호출 로직 변경, secret 값 커밋, migration 변경

## 1. 현재 프로젝트 적용 상태

현재 백엔드는 다음 환경변수를 읽는다.

| 환경변수 | 사용 위치 | 현재 용도 |
| --- | --- | --- |
| `NAVER_CLIENT_ID` | `backend/src/main/resources/application.yml` | 네이버 개발자센터 방식 Search API / DataLab API client id |
| `NAVER_CLIENT_SECRET` | `backend/src/main/resources/application.yml` | 네이버 개발자센터 방식 Search API / DataLab API client secret |
| `NAVER_DATALAB_DAILY_QUOTA` | `backend/src/main/resources/application.yml` | DataLab 일일 호출 제한 기본값 |
| `NAVER_PROVIDER_MODE` | 일반 기본 `LEGACY`, 운영 기본 `DISABLED` | `LEGACY`, `HUB`, `DISABLED` 중 선택 |
| `NAVER_API_HUB_CLIENT_ID` | API HUB mode 전용 | `X-NCP-APIGW-API-KEY-ID` 값 |
| `NAVER_API_HUB_CLIENT_SECRET` | API HUB mode 전용 | `X-NCP-APIGW-API-KEY` 값 |
| `NAVER_API_HUB_SHOPPING_SEARCH_ENDPOINT` | API HUB 쇼핑 검색 capability | 공식 확인한 전체 HTTP(S) endpoint |
| `NAVER_API_HUB_SHOPPING_INSIGHT_ENDPOINT` | API HUB 쇼핑인사이트 capability | 기본값은 공식 키워드 트렌드 endpoint, 필요 시 재정의 |

샘플 값 위치:

```text
.env.example
README.md
backend/src/main/resources/application.yml
```

실제 값은 `.env.example`, README, 코드, 테스트 fixture, PR 본문에 넣지 않는다.

## 2. 중요한 변경: NAVER API HUB 이관

네이버 개발자센터 공지 기준으로 Search API, Search Trend API, Shopping Insight API는 NAVER API HUB로 이관된다.

주요 일정:

| 날짜 | 내용 |
| --- | --- |
| 2026-06-25 | NAVER API HUB 정식 출시 |
| 2026-07-31 | 개발자센터에서 Search API, Search Trend API, Shopping Insight API 신규 신청 차단 |
| 2027-06-30 | 개발자센터 내 3개 API 지원 종료 |

따라서 새 운영 연동은 NAVER API HUB 기준으로 검토한다. 기존 개발자센터 key는 2026-07-31 이전 발급분에 한해 유예 기간 동안 사용할 수 있지만, 2027-06-30 이후에는 차단될 수 있다.

현재 코드는 provider port와 capability router를 통해 `LEGACY`, `HUB`, `DISABLED` mode를 분리한다. HUB adapter는 전용 credential과 `X-NCP-APIGW-API-KEY-ID`, `X-NCP-APIGW-API-KEY` header를 사용한다. Shopping Insight 공식 endpoint는 기본값으로 제공하고 Shopping Search는 endpoint가 확인될 때까지 비활성 상태를 유지한다.

## 3. 네이버 Search / Shopping Insight 준비

### 3.1 신규 신청 권장 경로

1. NAVER Cloud Platform 계정을 준비한다.
2. NAVER API HUB 서비스를 신청한다.
3. Application에서 `쇼핑인사이트` API를 선택한다.
4. API HUB 발급 key와 호출 방식 문서를 별도 보안 저장소에 기록한다.
5. API HUB key를 기존 `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`에 매핑하지 않는다.
6. 프로젝트 루트 `.env`에 `NAVER_PROVIDER_MODE=HUB`와 API HUB 전용 credential을 입력한다. 쇼핑 검색 endpoint는 비워 둔다.

### 3.2 로컬 `.env` 적용

Spring Boot는 실행 위치에 따라 프로젝트 루트의 `.env` 또는 상위 경로의 `.env`를 선택적으로 읽는다. 실제 파일은 `.gitignore`에 포함되며 저장소에 커밋하지 않는다.

```dotenv
NAVER_PROVIDER_MODE=HUB
NAVER_API_HUB_CLIENT_ID=발급값
NAVER_API_HUB_CLIENT_SECRET=발급값
NAVER_API_HUB_SHOPPING_INSIGHT_ENDPOINT=https://naverapihub.apigw.ntruss.com/shopping/v1/category/keywords
NAVER_API_HUB_SHOPPING_SEARCH_ENDPOINT=
```

2026-07-12 실제 credential로 인증과 HTTP 200 응답을 확인했다. 당시 공식 예제와 일반 키워드 모두 `results[].data`가 빈 배열이었으므로, 애플리케이션은 이를 성공·수집 0건으로 처리하고 데이터가 있다고 추정하지 않는다.

### 3.3 기존 개발자센터 key가 있는 경우

기존 개발자센터 key를 유예 기간 동안 사용하려면 다음을 확인한다.

1. 네이버 개발자센터의 Application > 내 애플리케이션에서 client id와 client secret을 확인한다.
2. 사용 API에 검색 또는 데이터랩 쇼핑인사이트가 포함되어 있는지 확인한다.
3. 비로그인 오픈 API 서비스 환경에 개발/운영 도메인이 등록되어 있는지 확인한다.
4. 로컬 실행 시에만 환경변수로 주입한다.

PowerShell 예:

```powershell
$env:NAVER_CLIENT_ID="issued-client-id"
$env:NAVER_CLIENT_SECRET="issued-client-secret"
$env:NAVER_DATALAB_DAILY_QUOTA="1000"
```

IntelliJ 실행 시:

```text
Run/Debug Configurations > backend 실행 구성 > Environment variables
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
NAVER_DATALAB_DAILY_QUOTA=1000
```

Docker/배포 시:

```text
배포 환경의 secret manager 또는 runtime environment variable로만 주입
이미지, compose override, repository 파일에 실제 값 저장 금지
```

## 4. SmartStore Commerce API 준비

SmartStore Commerce API는 네이버 개발자센터 Search/DataLab 방식과 다르다.

공식 문서 기준:

- Commerce API는 OAuth2 client credentials 방식을 사용한다.
- token endpoint는 `https://api.commerce.naver.com/external/v1/oauth2/token`이다.
- API 호출 시 `Authorization: Bearer {token}` header를 사용한다.

현재 프로젝트에는 mockable adapter와 skeleton이 있으며, 실제 Commerce API credential을 읽는 구현은 아직 없다.

따라서 다음 값은 아직 코드에 추가하지 않는다.

```text
NAVER_COMMERCE_CLIENT_ID
NAVER_COMMERCE_CLIENT_SECRET
NAVER_COMMERCE_ACCOUNT_ID
NAVER_COMMERCE_SELLER_ID
```

Commerce API 실제 연동 작업을 시작할 때 별도 이슈에서 다음을 먼저 정한다.

1. credential 환경변수 이름
2. token 발급/갱신 책임 위치
3. token 저장 여부와 암호화 정책
4. 401 재발급 fallback
5. API 호출 로그 저장 기준
6. 테스트는 MockWebServer 또는 mock adapter로 구성

## 5. 보안 원칙

- 실제 key, secret, token은 절대 commit하지 않는다.
- `.env.example`에는 빈 값 또는 placeholder만 둔다.
- GitHub issue, PR 본문, 테스트 fixture, 로그에 secret을 넣지 않는다.
- `Authorization`, cookie, API secret, raw token은 log masking 대상이다.
- 로컬 공유가 필요하면 메신저가 아니라 password manager 또는 secret manager를 사용한다.
- key가 노출되면 즉시 재발급하고 기존 key를 폐기한다.

## 6. 다음 구현 전 체크리스트

- [x] 신규 운영은 NAVER API HUB 우선, 기존 개발 호환은 LEGACY mode로 분리
- [x] provider mode와 capability 기반 활성화 구현
- [ ] 공식 NAVER API HUB 쇼핑 검색 endpoint 확인 후 환경별 주입
- [x] 공식 NAVER API HUB 쇼핑인사이트 endpoint 확인 및 adapter 계약 반영
- [ ] 운영 계정 소유자를 개인 계정이 아닌 조직/단체 계정으로 정리
- [ ] 로컬, staging, production key를 분리
- [ ] 호출 quota와 비용 상한 확인
- [x] 실패/429/403 응답을 `api_call_logs`에 남기는 기준 확인
- [x] 외부 API client 테스트는 실제 호출 없이 mock으로 작성
- [ ] 프론트엔드에서 네이버 API를 직접 호출하지 않는지 확인

## 7. 공식 참고 링크

- 네이버 개발자센터 Open API 사전 준비 사항: https://developers.naver.com/docs/common/openapiguide/appregister.md
- 네이버 쇼핑 검색 API: https://developers.naver.com/docs/serviceapi/search/shopping/shopping.md
- 네이버 쇼핑인사이트 API: https://developers.naver.com/docs/serviceapi/datalab/shopping/shopping.md
- Search API / Search Trend / Shopping Insight 이관 공지: https://developers.naver.com/notice/article/32530
- NAVER API HUB: https://www.ncloud.com/product/applicationService/naverApiHub
- NAVER API HUB 개요: https://api.ncloud-docs.com/docs/naver-api-hub-overview
- 네이버 커머스API 인증: https://apicenter.commerce.naver.com/docs/auth

## 8. 결론

신규 운영 연동은 NAVER API HUB 기준으로 진행한다. production은 provider가 기본 `DISABLED`이므로, API HUB credential과 공식 확인한 기능별 endpoint를 모두 주입한 capability만 활성화된다.

기존 개발자센터 key는 로컬 호환을 위한 `LEGACY` mode에서만 사용한다. 쇼핑 검색 HUB endpoint가 공식적으로 확인되기 전에는 값을 추측하지 않고 capability를 비활성으로 유지한다.

## 9. 로컬 응답 확인 시 인코딩 주의

PowerShell에서 한글 JSON을 문자열로 전달하면 실행 환경에 따라 keyword가 `?`로 변환되어 정상 credential에서도 빈 `data`가 반환될 수 있다. 실제 API 장애나 quota 문제로 판단하기 전에 UTF-8 byte body로 재확인한다.

UTF-8 요청으로 Shopping Insight의 일별 상대 지수가 정상 반환되는 것을 확인했으며, 이 값은 검색 클릭 추이이지 판매량이 아니다. 애플리케이션은 원본 응답을 프론트에 직접 노출하지 않고 PostgreSQL snapshot에 저장한 뒤 최신 기준일 묶음만 추천 점수와 키워드 상세 화면에 사용한다.
