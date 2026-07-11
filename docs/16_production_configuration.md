# Seller Radar 운영 프로필과 secret 주입 가이드

## 1. 목적

`prod` 프로필은 개발용 기본 credential, 빈 secret 또는 안전하지 않은 데이터베이스 설정으로 애플리케이션이 기동되는 것을 차단한다. 실제 credential은 저장소, 이미지, 로그, 이슈 또는 PR에 기록하지 않고 배포 환경의 secret manager에서 runtime에 주입한다.

## 2. 필수 환경변수

| 환경변수 | 용도 | 운영 기준 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring 프로필 | `prod` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://`로 시작 |
| `DB_USERNAME` | 애플리케이션 DB 사용자 | 환경별 전용 사용자 |
| `DB_PASSWORD` | 애플리케이션 DB 비밀번호 | 개발 기본값과 공통 비밀번호 금지 |
| `JWT_SECRET` | JWT HMAC key | UTF-8 기준 32바이트 이상, 환경별 분리 |

NAVER credential은 provider 전환 작업에서 capability와 함께 별도로 검증한다. R0-02에서는 외부 provider가 비활성 또는 미설정이어도 핵심 애플리케이션이 기동될 수 있도록 범위를 분리한다.

## 3. fail-fast 정책

`prod` 프로필에서는 애플리케이션 컨텍스트와 DB connection pool 초기화 전에 다음을 검사한다.

- 필수 설정 누락 또는 공백
- PostgreSQL이 아닌 JDBC URL
- `seller/seller` 개발 credential 조합
- 흔한 기본 DB 비밀번호
- 32바이트 미만 JWT secret
- `replace-with`, `test-only`, `example`, `placeholder`가 포함된 JWT placeholder

검증 오류에는 설정 키와 실패 이유만 포함하고 credential 원문은 포함하지 않는다.

## 4. 운영 고정값

`backend/src/main/resources/application-prod.yml`은 다음 값을 운영 기준으로 고정한다.

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: false
  jpa:
    hibernate:
      ddl-auto: validate

management:
  endpoints:
    web:
      exposure:
        include: health
```

- Flyway 이력 없는 기존 DB를 자동 baseline 처리하지 않는다.
- Hibernate는 운영 schema를 생성하거나 변경하지 않고 검증만 한다.
- Actuator는 `health`만 외부 노출 대상으로 둔다.
- 오류 응답에 내부 message, binding detail, stack trace를 포함하지 않는다.

### 4.1 인증 요청 제한

인증 요청 제한은 기본적으로 로그인 계정 5회/IP 20회/15분, 회원가입 계정 3회/IP 10회/1시간을 적용한다. 비밀번호 재설정 액션은 엔드포인트 도입 전까지 정책만 준비하며 계정 3회/IP 10회/1시간으로 설정한다.

현재 카운터는 단일 애플리케이션 인스턴스의 bounded in-memory 저장소를 사용한다. 수평 확장 전에는 sticky routing으로 보안 경계가 약화되지 않도록 단일 인스턴스로 운영하고, 다중 인스턴스 전환 시 중앙 저장소 기반 limiter로 교체한다. 최대 추적 키 수를 넘으면 새 키를 허용하지 않는 fail-closed 정책을 사용한다.

클라이언트 IP는 애플리케이션 서버가 관측한 원격 주소를 사용한다. reverse proxy의 전달 헤더를 사용할 때는 신뢰 가능한 proxy 대역과 Spring forwarded-header 전략을 함께 구성한 뒤 별도 검증한다.

### 4.2 업로드 quarantine

| 환경변수 | 기본값 | 운영 기준 |
|---|---|---|
| `UPLOAD_MAX_FILE_SIZE` | `10MB` | Spring multipart와 service admission에 동일하게 적용 |
| `UPLOAD_MAX_REQUEST_SIZE` | `10MB` | edge/proxy도 같거나 더 작은 값으로 설정 |
| `UPLOAD_QUARANTINE_DIR` | `./data/quarantine` | webroot 밖의 전용 private volume 절대 경로 권장 |
| `UPLOAD_RAW_RETENTION` | `P7D` | 원본 파일 보관 기간 |
| `UPLOAD_RAW_CLEANUP_BATCH_SIZE` | `100` | 일일 cleanup 한 번에 처리할 metadata 수 |

- quarantine volume은 애플리케이션 runtime identity만 읽고 쓸 수 있게 권한을 제한한다.
- 정적 파일 serving 경로와 같은 volume 또는 하위 경로를 사용하지 않는다.
- 외부 object storage로 전환할 때도 bucket/object는 public access를 차단하고 UUID key를 유지한다.
- 매일 UTC 03:00 cleanup이 만료 원본을 삭제한다. 실패 시 `raw_delete_failed_at`을 기록하고 다음 실행에서 재시도한다.
- XLSX parser hard limit 기본값은 ZIP entry 1,000개, 압축 해제 50 MB, inflate ratio 0.01, sheet 10개, row 20,000개, column 100개, cell 10,000자, 추출 text 20 MB다.
- local volume을 공유하지 않는 다중 인스턴스 배포 전에는 object storage로 전환하거나 cleanup 작업이 object 소유 인스턴스에 라우팅되도록 보장한다.

### 4.3 CSP, 보안 헤더, CORS

| 환경변수 | 기본값 | 운영 기준 |
|---|---|---|
| `CSP_ENFORCE` | 일반 프로필 `false`, prod `true` | beta 관찰 후 production enforce |
| `CSP_REPORT_URI` | 빈 값 | HTTPS report collector를 운영할 때만 설정 |
| `CORS_ALLOWED_ORIGINS` | 일반 프로필 localhost, prod 빈 값 | production Web origin을 쉼표로 구분해 명시 |
| `VITE_API_BASE_URL` | 미설정 시 앱 기본값은 local API | production build에서 실제 HTTPS API base URL 필수 |
| `VITE_CSP_REPORT_URI` | 빈 값 | 정적 프론트 CSP report collector |

- API는 `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, restrictive `Permissions-Policy`, `Cross-Origin-Opener-Policy: same-origin`을 반환한다.
- prod API와 정적 프론트는 enforce CSP와 HSTS를 반환한다. 일반 API와 Vite dev server는 Report-Only CSP를 사용한다.
- frontend build는 배포용 `dist/_headers`를 생성한다. `_headers`를 지원하지 않는 CDN·reverse proxy에서는 같은 값을 해당 플랫폼 설정으로 옮겨야 한다.
- `VITE_API_BASE_URL`이 없거나 유효한 HTTP(S) URL이 아니면 정적 CSP의 `connect-src`는 `'self'`만 허용해 외부 연결을 fail-closed로 차단한다.
- credentialed CORS에 `*`를 설정하면 애플리케이션이 기동을 거부한다. production origin은 `https://` 정확한 origin 단위로 지정한다.
- 외부 상품 링크와 이미지는 backend 저장 경계와 frontend 렌더링 경계에서 `http`/`https`만 허용하며 user-info URL을 거부한다.

## 5. 배포 절차

1. 배포 플랫폼의 secret manager에 환경별 credential을 등록한다.
2. runtime identity가 필요한 secret만 읽을 수 있도록 최소 권한을 부여한다.
3. `SPRING_PROFILES_ACTIVE=prod`와 필수 환경변수를 runtime에 주입한다.
4. 배포 전 백업과 현재 Flyway version을 확인한다.
5. 애플리케이션을 기동하고 `/actuator/health`를 확인한다.
6. 기동 실패 시 설정 키와 원인만 확인하고 secret 값을 로그나 이슈에 복사하지 않는다.

## 6. 검증 명령

Docker가 실행 중인 개발 환경에서 다음 명령으로 production profile과 PostgreSQL migration을 함께 검증한다.

```powershell
cd backend
.\gradlew.bat clean test
.\gradlew.bat build
```

통합 테스트는 임시 PostgreSQL 16 container와 테스트 전용 credential을 생성한다. 실제 운영 credential이나 외부 API를 사용하지 않는다.

## 7. 금지 사항

- `application-prod.yml`에 credential 기본값 추가
- secret을 명령행 literal, shell history, 로그 또는 PR에 기록
- 운영에서 `JPA_DDL_AUTO=update` 사용
- 검증 없이 `FLYWAY_BASELINE_ON_MIGRATE=true` 활성화
- local, staging, production credential 재사용
