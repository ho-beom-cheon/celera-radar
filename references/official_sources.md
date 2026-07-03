# Official Source Notes

Checked date: 2026-07-02

## Naver Shopping Search API

- URL: https://developers.naver.com/docs/serviceapi/search/shopping/shopping.md
- Notes:
  - Returns Naver shopping search results as XML or JSON.
  - Daily search API limit: 25,000 calls.
  - `display` max: 100.
  - `start` max: 1000.
  - `sort` supports sim/date/asc/dsc.
  - Header authentication uses X-Naver-Client-Id and X-Naver-Client-Secret.

## Naver DataLab Shopping Insight API

- URL: https://developers.naver.com/docs/serviceapi/datalab/shopping/shopping.md
- Notes:
  - Returns search click trend ratio data for Naver integrated search shopping area and Naver Shopping.
  - Daily limit: 1,000 calls.
  - Ratio values are relative click trend values, not sales volume.
  - Header authentication uses X-Naver-Client-Id and X-Naver-Client-Secret.

## Apps in Toss WebView

- URL: https://developers-apps-in-toss.toss.im/tutorials/webview.html
- Notes:
  - Existing web projects can install Apps in Toss Web Framework SDK.
  - TDS WebView packages can be used for Toss Design System components.
  - Build bundle is uploaded and tested through Apps in Toss console.

## Apps in Toss Service Open Policy

- URL: https://developers-apps-in-toss.toss.im/intro/guide.html
- Notes:
  - Self-app install inducement and external link movement are restricted.
  - Main functions configured for the mini-app should be completed inside the mini-app.
  - Toss login/payment/ads methods are constrained by platform rules.

## Apps in Toss In-App Purchase

- URL: https://developers-apps-in-toss.toss.im/iap/intro.html
- Notes:
  - Used for digital products, rights, and content.
  - Business and settlement information registration is required.
  - Non-game mini-apps can register up to 30 in-app products.
  - Auto-renewing subscription is supported as a product type.

## Naver Commerce API

- URL: https://apicenter.commerce.naver.com/docs/introduction
- Notes:
  - Allows SmartStore major functions/content to be called through HTTP API.
  - Requires Commerce API Center registration, app registration, permission acquisition, and authentication.

## Naver Commerce API Commission Details

- URL: https://apicenter.commerce.naver.com/docs/commerce-api/current/find-commission-details-pay-settle
- Notes:
  - Provides the commission-details endpoint under settlement APIs.
  - Planned for v1.5 or later, not MVP.
