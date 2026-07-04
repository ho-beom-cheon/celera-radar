# 셀러레이더 인터페이스 설계서

- 문서 버전: v0.1
- 작성일: 2026-07-02
- 대상 단계: Web SaaS v0.1~v0.4, 앱인토스 Lite v1.0, 향후 스마트스토어 API v1.5
- 문서 목적: 프론트엔드, 백엔드, 외부 API, 배치 작업 사이의 계약을 정의한다.

---

## 1. 인터페이스 범위

| 구분 | 포함 | MVP 여부 |
|---|---|---|
| Web Frontend ↔ Backend REST API | 인증, 키워드, 분석결과, CSV, 후보, 알림 | 포함 |
| 앱인토스 Lite ↔ Backend REST API | 추천 카드, 빠른 마진 계산, 관심 키워드 | v1.0 |
| Backend ↔ 네이버 쇼핑 검색 API | 가격대/상위 상품 수집 | v0.1 |
| Backend ↔ 네이버 데이터랩 쇼핑인사이트 API | 키워드/카테고리 트렌드 수집 | v0.2 |
| Backend ↔ 도매 CSV | 파일 업로드/파싱 | v0.3 |
| Backend ↔ 앱인토스 인앱결제 | 유료 기능 결제/권한 | v1.0 이후 |
| Backend ↔ 네이버 커머스API | 상품/주문/정산/수수료 | v1.5 이후 |

---

## 2. 공식 문서 확인 사항

| 외부 인터페이스 | 확인 내용 | 설계 반영 |
|---|---|---|
| 네이버 쇼핑 검색 API | 하루 호출 한도 25,000회, display 최대 100, start 최대 1000, sort asc/dsc 지원 | 서버 배치/캐시, 가격 오름차순 분석 |
| 네이버 데이터랩 쇼핑인사이트 API | 하루 호출 한도 1,000회, 검색 클릭 추이 ratio 제공 | 일일 배치, 키워드 우선순위, 실제 판매량 아님 표시 |
| 앱인토스 WebView | 기존 웹 프로젝트에 SDK 설치, TDS WebView 패키지 사용 가능 | 별도 Lite 화면 구성 |
| 앱인토스 정책 | 자사 앱 설치 유도/외부 링크 이동 제한, 앱 내 기능 완결 필요 | Lite 기능은 토스 안에서 완결 |
| 앱인토스 인앱결제 | 사업자/정산 정보 필요, 비게임 상품 최대 30개, 자동 갱신 구독 가능 | BASIC/PRO 구독 상품 설계 |
| 네이버 커머스API | 스마트스토어 주요 기능을 HTTP API로 호출, 센터 가입/앱 등록/권한/인증 필요 | v1.5 이후 연동 |

---

## 3. 공통 API 규칙

### 3.1 Base URL

```text
개발: http://localhost:8080/api/v1
운영: https://api.seller-radar.example.com/api/v1
```

### 3.2 인증

MVP Web SaaS는 JWT Bearer 토큰을 사용한다.

```http
Authorization: Bearer {accessToken}
```

앱인토스 Lite는 추후 토스 로그인 식별값과 내부 user_id를 매핑한다. 초기 v1.0 전까지는 Web 계정과 분리할 수 있다.

### 3.3 응답 Envelope

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": {
    "requestId": "req_20260702_000001",
    "cached": true,
    "generatedAt": "2026-07-02T07:30:00+09:00"
  }
}
```

오류 응답:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "KEYWORD_LIMIT_EXCEEDED",
    "message": "현재 요금제에서 등록 가능한 키워드 수를 초과했습니다.",
    "field": "keyword"
  },
  "meta": {
    "requestId": "req_20260702_000002"
  }
}
```

### 3.4 Pagination

```http
GET /keywords?page=0&size=20&sort=createdAt,desc
```

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 130,
  "totalPages": 7
}
```

### 3.5 시간대

- 서버 저장: UTC 권장
- 사용자 표시: Asia/Seoul
- 배치 기준일: Asia/Seoul

---

## 4. 내부 REST API

## 4.1 Auth API

### POST /auth/signup

회원가입.

Request:

```json
{
  "email": "seller@example.com",
  "password": "password1234",
  "termsAgreed": true
}
```

Response:

```json
{
  "userId": 1,
  "email": "seller@example.com",
  "plan": "FREE",
  "accessToken": "jwt...",
  "refreshToken": "jwt..."
}
```

### POST /auth/login

Request:

```json
{
  "email": "seller@example.com",
  "password": "password1234"
}
```

---

## 4.2 Keyword API

> P1-002 기준: Keyword REST API는 `categoryCode`/`priority`가 아니라 문자열 `category`를 사용한다.
> 목록 필터는 `analysisStatus=PENDING|RUNNING|SUCCESS|FAILED|SKIPPED`만 허용하며,
> 기본 조회는 `active=true` 및 `deleted_at is null`인 키워드만 반환한다.
> 삭제는 physical delete가 아니라 `active=false`, `deleted_at=now()` soft delete로 처리한다.

### GET /keywords

관심 키워드 목록 조회.

Query Parameters:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| page | number | N | 기본 0 |
| size | number | N | 기본 20 |
| category | string | N | 내부 카테고리 코드 |
| analysisStatus | string | N | PENDING/RUNNING/SUCCESS/FAILED/SKIPPED |

Response item:

```json
{
  "id": 101,
  "keyword": "차량용 먼지 브러쉬",
  "category": "CAR_ACCESSORY",
  "active": true,
  "analysisStatus": "SUCCESS",
  "lastAnalyzedAt": "2026-07-02T07:12:00+09:00",
  "lastSnapshotDate": "2026-07-02",
  "latestMinPrice": 4900,
  "latestAvgPrice": 12300,
  "latestCompetitionLevel": "HIGH",
  "createdAt": "2026-07-01T10:00:00+09:00",
  "updatedAt": "2026-07-02T07:12:00+09:00"
}
```

### GET /keywords/{keywordId}

키워드 단건 조회. 삭제된 키워드 또는 다른 사용자의 키워드는 `KEYWORD_NOT_FOUND`로 응답한다.

### POST /keywords

키워드 등록.

Request:

```json
{
  "keyword": "케이블 정리 트레이",
  "category": "DESK_OFFICE"
}
```

Validation:

| 항목 | 규칙 |
|---|---|
| keyword | 2~50자 |
| category | 100자 이하. null/빈 문자열/공백은 null 저장 |
| 중복 | 사용자별 active 키워드 중 normalized keyword 중복 불가 |
| 무료 제한 | FREE는 3개까지 |

### PUT /keywords/{keywordId}

키워드 수정.
Request는 `POST /keywords`와 동일하다. normalized keyword가 변경되면 같은 사용자 active 키워드와 중복되는지 확인한다.

### DELETE /keywords/{keywordId}

키워드 삭제. physical delete가 아니라 `active=false`, `deleted_at=now()`로 soft delete 처리한다.

---

## 4.3 Analysis API

### GET /keywords/{keywordId}/analysis

키워드 상세 분석 조회. 기존 호환 endpoint이며, 화면에서 통합 분석 상태를 조회할 때 사용한다.

Response:

```json
{
  "keywordId": 101,
  "keyword": "차량용 먼지 브러쉬",
  "status": "SUCCESS",
  "lastAnalyzedAt": "2026-07-02T07:12:00+09:00",
  "shopping": {
    "baseDate": "2026-07-02",
    "totalResults": 18230,
    "minPrice": 4900,
    "maxPrice": 29900,
    "avgPrice": 12300,
    "topItems": [
      {
        "title": "차량용 먼지 제거 브러쉬",
        "lprice": 4900,
        "mallName": "sample mall",
        "category1": "생활/건강",
        "category2": "자동차용품"
      }
    ]
  },
  "trend": {
    "timeUnit": "date",
    "points": [
      {"period": "2026-06-30", "ratio": 82.3},
      {"period": "2026-07-01", "ratio": 91.4}
    ],
    "trendDelta7d": 12.4,
    "trendDelta30d": 28.1
  },
  "score": {
    "trendScore": 22,
    "competitionScore": 16,
    "riskPenalty": -3,
    "overallScore": 74,
    "grade": "REVIEW",
    "reasons": ["최근 30일 검색 관심 상승", "가격대가 1만 원대에 형성"],
    "warnings": ["상위 상품 리뷰 수 확인 필요", "실제 판매량 데이터 아님"]
  }
}
```

분석 스냅샷이 아직 없으면 `shopping`, `trend`, `score`는 `null`로 응답한다.

### POST /keywords/{keywordId}/analyze/shopping

네이버 쇼핑 검색 snapshot 분석을 즉시 실행한다. 같은 `keyword_id + search_date + sort_type` 성공 snapshot이 이미 있으면 외부 API를 다시 호출하지 않고 캐시 결과를 반환한다.

Response:

```json
{
  "keywordId": 101,
  "keyword": "차량용 먼지 브러쉬",
  "searchDate": "2026-07-03",
  "sortType": "sim",
  "cached": false,
  "totalCount": 18230,
  "minPrice": 4900,
  "maxPrice": 29900,
  "avgPrice": 12300,
  "competitionLevel": "HIGH",
  "fetchedAt": "2026-07-03T07:12:00+09:00",
  "topItems": [
    {
      "rankNo": 1,
      "title": "차량용 먼지 제거 브러쉬",
      "productUrl": "https://example.com/item",
      "imageUrl": "https://example.com/item.jpg",
      "lowPrice": 4900,
      "mallName": "sample mall",
      "category1": "생활/건강",
      "category2": "자동차용품",
      "category3": "",
      "category4": ""
    }
  ]
}
```

### GET /keywords/{keywordId}/shopping-snapshot/latest

최신 네이버 쇼핑 snapshot을 조회한다. snapshot이 없으면 `ANALYSIS_NOT_READY`를 반환한다.

Response는 `POST /keywords/{keywordId}/analyze/shopping`과 동일하다.

---

## 4.4 Candidate API

### GET /candidates

추천 후보 목록.

Query Parameters:

| 이름 | 설명 |
|---|---|
| grade | RECOMMENDED/REVIEW/HOLD/EXCLUDED |
| categoryCode | 카테고리 |
| minScore | 최소 점수 |
| minMarginRate | 최소 예상 마진율 |
| source | KEYWORD/CSV/API |

Response item:

```json
{
  "candidateId": 3001,
  "name": "차량용 먼지 브러쉬",
  "source": "CSV",
  "categoryCode": "CAR_ACCESSORY",
  "score": 82,
  "grade": "RECOMMENDED",
  "expectedSalePrice": 12900,
  "supplyPrice": 4200,
  "shippingFee": 3000,
  "expectedMarginRate": 31.2,
  "riskLevel": "LOW",
  "status": "ACTIVE",
  "createdAt": "2026-07-02T07:45:00+09:00"
}
```

### GET /candidates/{candidateId}

후보 상세.

Response:

```json
{
  "candidateId": 3001,
  "name": "차량용 먼지 브러쉬",
  "source": "CSV",
  "categoryCode": "CAR_ACCESSORY",
  "status": "ACTIVE",
  "score": 82,
  "grade": "RECOMMENDED",
  "expectedSalePrice": 12900,
  "supplyPrice": 4200,
  "shippingFee": 3000,
  "expectedMarginRate": 31.2,
  "riskLevel": "LOW",
  "scoreBreakdown": {
    "trendScore": 24,
    "competitionScore": 18,
    "marginScore": 22,
    "priceBandScore": 10,
    "supplyScore": 5,
    "riskPenalty": 0
  },
  "reasons": [
    "검색 클릭 추이와 예상 마진이 기준을 충족합니다."
  ],
  "warnings": [
    "데이터랩 ratio는 검색 클릭 추이 기반이며 실제 판매량이 아닙니다."
  ],
  "keywordId": 101,
  "wholesaleProductId": 5001,
  "createdAt": "2026-07-02T07:45:00+09:00",
  "updatedAt": "2026-07-02T07:45:00+09:00"
}
```

### POST /candidates/{candidateId}/save

관심 후보 저장.

Response: `GET /candidates/{candidateId}`와 동일한 상세 응답을 반환하며 `status`는 `SAVED`다.

### POST /candidates/{candidateId}/exclude

후보 제외 처리.

Response: `GET /candidates/{candidateId}`와 동일한 상세 응답을 반환하며 `status`는 `EXCLUDED`다. 제외된 후보는 기본 후보 목록에서 숨긴다.

---

## 4.5 Wholesale CSV API

### POST /wholesale-uploads/preview

CSV/XLSX 파일을 저장하고, 컬럼 header와 preview row를 반환한다. 이 응답의 `uploadId`를 다음 confirm API에 사용한다.

Content-Type: multipart/form-data

Form fields:

| 필드 | 설명 |
|---|---|
| file | CSV 또는 XLSX 파일 |
| encoding | AUTO/UTF_8/CP949. XLSX에서는 무시 |
| sourceName | 도매처명 |

Response:

```json
{
  "uploadId": 501,
  "status": "UPLOADED",
  "fileType": "CSV",
  "detectedEncoding": "UTF_8",
  "preview": {
    "originalFilename": "items.csv",
    "fileType": "CSV",
    "headers": ["productName", "supplyPrice", "shippingFee"],
    "rowCount": 2,
    "rows": [
      {
        "rowNo": 2,
        "cells": [
          {
            "header": "supplyPrice",
            "rawValue": "1,200 원",
            "decimalValue": 1200,
            "longValue": 1200
          }
        ]
      }
    ]
  }
}
```

### POST /wholesale-uploads/{uploadId}/confirm

preview로 저장된 파일에 컬럼 매핑을 적용해 `wholesale_products`에 저장한다.

Request:

```json
{
  "mapping": {
    "productName": "상품명",
    "supplyPrice": "공급가",
    "shippingFee": "배송비",
    "imageUrl": "이미지URL",
    "productUrl": "상품URL",
    "category": "카테고리"
  }
}
```

Response:

```json
{
  "uploadId": 501,
  "successCount": 3100,
  "failureCount": 100,
  "failureReasons": [
    {
      "rowNo": 7,
      "message": "supplyPrice must be a positive number."
    }
  ]
}
```

### POST /wholesale-files

CSV 업로드.

Content-Type: multipart/form-data

Form fields:

| 필드 | 설명 |
|---|---|
| file | CSV 파일 |
| encoding | auto/utf-8/cp949 |
| sourceName | 도매처명 |

Response:

```json
{
  "fileId": 501,
  "status": "UPLOADED",
  "rowCount": 3200,
  "detectedColumns": ["상품명", "공급가", "배송비", "카테고리"]
}
```

### GET /wholesale-files/{fileId}

업로드 파일 메타를 조회한다. `storedPath` 같은 서버 내부 저장 경로는 응답하지 않는다.

### POST /wholesale-files/{fileId}/column-mapping

Request:

```json
{
  "mapping": {
    "productName": "상품명",
    "supplyPrice": "공급가",
    "shippingFee": "배송비",
    "category": "카테고리",
    "productUrl": "상품URL"
  }
}
```

### POST /wholesale-files/{fileId}/parse

CSV 파싱 작업 시작.

Response:

```json
{
  "fileId": 501,
  "parsedCount": 3100,
  "invalidCount": 100
}
```

### GET /wholesale-files/{fileId}/rows

파싱 결과 행 조회.

### POST /wholesale-files/{fileId}/candidates

파싱된 정상 CSV 행을 상품 후보로 변환한다. 같은 CSV 행으로 이미 후보가 생성된 경우 중복 생성하지 않는다.

Response:

```json
{
  "fileId": 501,
  "generatedCount": 25,
  "skippedCount": 3
}
```

---

## 4.6 Margin API

### POST /margin/calculate

빠른 마진 계산.

Request:

```json
{
  "salePrice": 39900,
  "supplyPrice": 22000,
  "platformFeeRate": 4.0,
  "shippingFee": 3000,
  "packagingFee": 500,
  "adCostPerOrder": 2000,
  "couponDiscount": 1000,
  "targetMarginRate": 25.0
}
```

Response:

```json
{
  "salePrice": 39900,
  "totalCost": 30096,
  "platformFee": 1596,
  "expectedProfit": 9804,
  "expectedMarginRate": 24.57,
  "status": "WARNING",
  "recommendedSalePrice": 40500,
  "maxAllowedAdCost": 1820,
  "maxAllowedCoupon": 1420
}
```

구현 시 `totalCost`는 정확한 계산식 테스트로 검증한다. 문서 예시는 구조 설명용이다.

---

## 4.7 Alert API

### GET /alerts

알림 목록.

Response item:

```json
{
  "id": 9001,
  "type": "CANDIDATE_SCORE",
  "status": "UNREAD",
  "title": "검토 후보 조건에 맞는 상품이 있습니다.",
  "message": "차량용 먼지 브러쉬 후보가 알림 조건을 충족했습니다. 점수와 마진을 확인하세요.",
  "candidateId": 3001,
  "candidateName": "차량용 먼지 브러쉬",
  "ruleId": 7001,
  "ruleName": "추천점수 80 이상",
  "createdAt": "2026-07-02T08:00:00+09:00",
  "readAt": null
}
```

### POST /alert-rules

알림 조건 생성.

Request:

```json
{
  "name": "추천점수 80 이상",
  "minScore": 80,
  "minMarginRate": 25,
  "categoryCodes": ["CAR_ACCESSORY", "DESK_OFFICE"],
  "riskExcluded": true,
  "frequency": "DAILY_SUMMARY"
}
```

### PATCH /alerts/{alertId}/read

읽음 처리.

Response: `GET /alerts`와 동일한 알림 단건 응답을 반환하며 `status`는 `READ`다.

---

## 4.8 Subscription API

MVP에서는 서버 내부 플랜만 관리한다. 앱인토스 결제 연동 이후 영수증/권한 검증을 추가한다.

### GET /me/subscription

Response:

```json
{
  "plan": "FREE",
  "keywordLimit": 3,
  "csvRowLimit": 100,
  "candidateLimit": 10,
  "features": ["QUICK_MARGIN", "LIMITED_KEYWORDS"]
}
```

---

## 4.9 Batch Admin API

관리자 전용. MVP에서는 내부 접근만 허용한다.

| Method | Path | 설명 |
|---|---|---|
| GET | /admin/batches | 배치 실행 이력 |
| POST | /admin/batches/shopping-search/run | 쇼핑 검색 배치 수동 실행 |
| POST | /admin/batches/datalab/run | 데이터랩 배치 수동 실행 |
| GET | /admin/api-call-logs | 외부 API 호출 로그 |

### POST /admin/batches/shopping-search/run

관리자 권한이 필요하다. ACTIVE 키워드만 대상으로 쇼핑 검색 스냅샷 수집을 즉시 실행한다.

Response:

```json
{
  "id": 1,
  "jobType": "SHOPPING_SEARCH_DAILY",
  "triggerType": "MANUAL",
  "status": "SUCCESS",
  "targetCount": 10,
  "successCount": 10,
  "failureCount": 0,
  "startedAt": "2026-07-02T06:30:00+09:00",
  "finishedAt": "2026-07-02T06:30:05+09:00",
  "errorMessage": null
}
```

### POST /admin/batches/datalab/run

관리자 권한이 필요하다. ACTIVE keyword를 대상으로 데이터랩 트렌드 스냅샷 수집 batch를 즉시 실행한다. 처리 대상 수는 `DATALAB_DAILY_TARGET_LIMIT` 설정값으로 제한한다.

Response:

```json
{
  "id": 2,
  "jobType": "DATALAB_TREND_DAILY",
  "triggerType": "MANUAL",
  "status": "SUCCESS",
  "targetCount": 10,
  "successCount": 10,
  "failureCount": 0,
  "startedAt": "2026-07-02T07:00:00+09:00",
  "finishedAt": "2026-07-02T07:00:05+09:00",
  "errorMessage": null
}
```

### GET /admin/batches

관리자 권한이 필요하다. 최신 실행 이력부터 페이지 단위로 조회한다.

Response:

```json
{
  "items": [
    {
      "id": 1,
      "jobType": "SHOPPING_SEARCH_DAILY",
      "triggerType": "MANUAL",
      "status": "SUCCESS",
      "targetCount": 10,
      "successCount": 10,
      "failureCount": 0,
      "startedAt": "2026-07-02T06:30:00+09:00",
      "finishedAt": "2026-07-02T06:30:05+09:00",
      "errorMessage": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 5. 외부 API 인터페이스

## 5.1 네이버 쇼핑 검색 API

### 용도

- 키워드별 네이버 쇼핑 검색 결과 조회
- 최저가, 상품 카테고리, 상위 상품, 검색 결과 수 저장
- 경쟁강도 계산 자료로 사용

### 요청

```http
GET https://openapi.naver.com/v1/search/shop.json?query={keyword}&display=100&start=1&sort=sim&exclude=used:rental:cbshop
X-Naver-Client-Id: {clientId}
X-Naver-Client-Secret: {clientSecret}
```

### 설계 원칙

1. 사용자 요청마다 호출하지 않는다.
2. 키워드별 하루 1회 이하로 호출한다.
3. 응답 원문 일부를 JSONB로 저장한다.
4. 실패 시 마지막 성공 snapshot을 사용한다.
5. 429/403/500은 api_call_log와 external_api_error_log에 기록한다.

### 저장 테이블

- shopping_search_snapshots
- shopping_item_snapshots
- api_call_logs

---

## 5.2 네이버 데이터랩 쇼핑인사이트 API

### 용도

- 쇼핑 검색 클릭 추이 ratio 수집
- 키워드/카테고리별 트렌드 점수 계산

### 주의

이 데이터는 실제 판매량이 아니다. 화면과 리포트에 “검색 클릭 추이 기반”임을 표시한다.

### 요청 예시

```http
POST https://openapi.naver.com/v1/datalab/shopping/category/keywords
X-Naver-Client-Id: {clientId}
X-Naver-Client-Secret: {clientSecret}
Content-Type: application/json
```

```json
{
  "startDate": "2026-06-02",
  "endDate": "2026-07-01",
  "timeUnit": "date",
  "category": "50000000",
  "keyword": [
    {
      "name": "차량용 수납함",
      "param": ["차량용 수납함"]
    }
  ],
  "device": "",
  "gender": "",
  "ages": []
}
```

### 호출 한도 설계

- 공식 한도: 하루 1,000회
- api_call_log 기준으로 당일 호출 수를 계산한다.
- v0.2 운영 기준: HIGH 우선순위 키워드 300~500개 일 1회
- LOW 우선순위 키워드는 주 1~2회 순환 분석
- 사용자 키워드는 등록 즉시 분석하지 않고 다음 배치에 반영

---

## 5.3 앱인토스 WebView / 인앱결제

### WebView

- 기존 React 프로젝트에 앱인토스 Web SDK를 설치해 Lite 화면을 구성한다.
- TDS WebView 컴포넌트를 적용한다.
- 앱인토스 화면은 핵심 기능을 외부 이동 없이 완료해야 한다.

### 인앱결제

- 디지털 상품/권한 판매로 BASIC/PRO 30일 이용권 또는 자동 갱신 구독을 구성한다.
- 사업자 정보와 정산 정보 등록이 필요하다.
- 비게임 미니앱 인앱 상품 수 제한을 고려해 상품 수를 적게 유지한다.

---

## 5.4 향후 네이버 커머스API

v1.5 이후 연동한다.

| 영역 | 용도 |
|---|---|
| 상품 | 스마트스토어 상품 목록/판매가 동기화 |
| 주문 | 상품별 판매 수량 수집 |
| 정산 | 실제 정산금 확인 |
| 수수료 상세 | 예상 수수료와 실제 수수료 비교 |

주의: 매입가, 포장비, 기타 원가는 네이버 API로 알 수 없으므로 사용자 입력/CSV 관리가 필요하다.

---

## 6. 배치 인터페이스

## 6.1 Batch Job 목록

| Job ID | 주기 | 입력 | 출력 | 실패 대응 |
|---|---|---|---|---|
| SHOPPING_SEARCH_DAILY | 매일 06:30 | active keyword | shopping snapshot | 3회 재시도, 실패 로그 |
| DATALAB_TREND_DAILY | 매일 07:00 | priority keyword | trend snapshot | 호출 한도 체크 |
| SCORE_RECALC_DAILY | 매일 07:30 | snapshot/csv/risk rule | candidate score | 실패 후보만 재처리 |
| ALERT_GENERATE_DAILY | 매일 08:00 | candidate score/rule | alert | 중복 알림 방지 |
| CSV_PARSE_ON_DEMAND | 요청 시 | fileId | wholesale rows | 오류 행 저장 |

## 6.2 배치 상태값

| 상태 | 의미 |
|---|---|
| CREATED | 생성 |
| RUNNING | 실행 중 |
| SUCCESS | 성공 |
| PARTIAL_SUCCESS | 일부 실패 |
| FAILED | 실패 |
| SKIPPED | 호출 한도/조건 미충족으로 건너뜀 |

---

## 7. 에러 코드

| 코드 | HTTP | 설명 |
|---|---:|---|
| AUTH_REQUIRED | 401 | 인증 필요 |
| FORBIDDEN | 403 | 권한 없음 |
| DUPLICATED_EMAIL | 409 | 이미 가입된 이메일 |
| INVALID_CREDENTIALS | 401 | 로그인 정보 불일치 |
| INVALID_REFRESH_TOKEN | 401 | Refresh token 오류 |
| USER_NOT_FOUND | 404 | 사용자 없음 |
| KEYWORD_NOT_FOUND | 404 | 키워드 없음 |
| CANDIDATE_NOT_FOUND | 404 | 후보 없음 |
| WHOLESALE_FILE_NOT_FOUND | 404 | 도매 CSV 파일 없음 |
| ALERT_NOT_FOUND | 404 | 알림 없음 |
| KEYWORD_LIMIT_EXCEEDED | 400 | 요금제 키워드 한도 초과 |
| DUPLICATED_KEYWORD | 409 | 중복 키워드 |
| CSV_INVALID_FORMAT | 400 | CSV 형식 오류 |
| CSV_REQUIRED_COLUMN_MISSING | 400 | 필수 컬럼 누락 |
| CSV_ROW_LIMIT_EXCEEDED | 400 | 요금제 CSV 행 수 초과 |
| EXTERNAL_API_RATE_LIMIT | 429 | 외부 API 한도 초과 |
| EXTERNAL_API_UNAVAILABLE | 503 | 외부 API 장애 |
| ANALYSIS_NOT_READY | 202 | 분석 대기 중 |
| SUBSCRIPTION_REQUIRED | 402 | 유료 기능 필요 |

---

## 8. 데이터 보안 원칙

1. 네이버 API 키는 서버 환경변수로만 관리한다.
2. 프론트엔드에 외부 API 키를 노출하지 않는다.
3. CSV 업로드 파일은 원본 저장 기간을 제한한다. MVP 기준 30일 후 삭제 가능하게 설계한다.
4. 사용자 업로드 파일 URL은 외부에 공개하지 않는다.
5. 로그에는 API Secret, JWT, 개인정보를 남기지 않는다.

---

## 9. 참고 공식 문서

- 네이버 쇼핑 검색 API: https://developers.naver.com/docs/serviceapi/search/shopping/shopping.md
- 네이버 데이터랩 쇼핑인사이트 API: https://developers.naver.com/docs/serviceapi/datalab/shopping/shopping.md
- 앱인토스 WebView SDK 연동: https://developers-apps-in-toss.toss.im/tutorials/webview.html
- 앱인토스 서비스 오픈 정책: https://developers-apps-in-toss.toss.im/intro/guide.html
- 앱인토스 인앱 결제: https://developers-apps-in-toss.toss.im/iap/intro.html
- 네이버 커머스API 소개: https://apicenter.commerce.naver.com/docs/introduction
- 네이버 커머스API 수수료 상세 내역 조회: https://apicenter.commerce.naver.com/docs/commerce-api/current/find-commission-details-pay-settle
