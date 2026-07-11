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
