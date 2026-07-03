package com.sellerradar.shopping.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiProperties;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class NaverShoppingClientTest {
	private static final String CLIENT_ID = "test-client-id";
	private static final String CLIENT_SECRET = "test-client-secret";

	private MockWebServer mockWebServer;
	private NaverShoppingClient client;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		RestClient restClient = RestClient.builder()
				.baseUrl(mockWebServer.url("/").toString())
				.build();
		client = new NaverShoppingClient(restClient, new NaverApiProperties(CLIENT_ID, CLIENT_SECRET, 1000));
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void searchSendsHeadersAndQueryParameters() throws InterruptedException {
		mockWebServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("""
						{
						  "lastBuildDate": "Thu, 02 Jul 2026 20:00:00 +0900",
						  "total": 1234,
						  "start": 1,
						  "display": 2,
						  "items": [
						    {
						      "title": "<b>차량용 수납함</b>",
						      "link": "https://example.com/products/1",
						      "image": "https://example.com/products/1.jpg",
						      "lprice": "12900",
						      "hprice": "15900",
						      "mallName": "테스트몰",
						      "productId": "1000001",
						      "productType": "1",
						      "brand": "테스트브랜드",
						      "maker": "테스트제조사",
						      "category1": "자동차용품",
						      "category2": "수납용품",
						      "category3": "",
						      "category4": ""
						    }
						  ]
						}
						"""));

		NaverShoppingSearchResponse response = client.search(new NaverShoppingSearchRequest(
				"차량용 수납함",
				2,
				1,
				NaverShoppingSort.SIM,
				"used:rental:cbshop"
		));

		assertThat(response.total()).isEqualTo(1234);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().lprice()).isEqualTo("12900");
		assertThat(response.items().getFirst().mallName()).isEqualTo("테스트몰");

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).startsWith("/v1/search/shop.json?");
		assertThat(request.getHeader("X-Naver-Client-Id")).isEqualTo(CLIENT_ID);
		assertThat(request.getHeader("X-Naver-Client-Secret")).isEqualTo(CLIENT_SECRET);
		assertThat(request.getRequestUrl().queryParameter("query")).isEqualTo("차량용 수납함");
		assertThat(request.getRequestUrl().queryParameter("display")).isEqualTo("2");
		assertThat(request.getRequestUrl().queryParameter("start")).isEqualTo("1");
		assertThat(request.getRequestUrl().queryParameter("sort")).isEqualTo("sim");
		assertThat(request.getRequestUrl().queryParameter("exclude")).isEqualTo("used:rental:cbshop");
	}

	@Test
	void searchMapsRateLimitResponse() {
		mockWebServer.enqueue(new MockResponse()
				.setResponseCode(429)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("{}"));

		assertThatThrownBy(() -> client.search(new NaverShoppingSearchRequest(
				"차량용 수납함",
				10,
				1,
				NaverShoppingSort.SIM,
				null
		)))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));
	}

	@Test
	void searchRejectsMissingCredentialsBeforeRequest() {
		NaverShoppingClient clientWithoutCredentials = new NaverShoppingClient(
				RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build(),
				new NaverApiProperties("", "", 1000)
		);

		assertThatThrownBy(() -> clientWithoutCredentials.search(new NaverShoppingSearchRequest(
				"차량용 수납함",
				10,
				1,
				NaverShoppingSort.SIM,
				null
		)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("NAVER_CLIENT_ID");
		assertThat(mockWebServer.getRequestCount()).isZero();
	}
}
