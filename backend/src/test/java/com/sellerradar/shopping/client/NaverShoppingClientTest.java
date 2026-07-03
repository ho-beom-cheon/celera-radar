package com.sellerradar.shopping.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiProperties;
import java.io.IOException;
import okhttp3.HttpUrl;
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
	void searchSendsHeadersQueryParametersAndMapsResponse() throws InterruptedException {
		mockWebServer.enqueue(successResponse());

		NaverShoppingSearchResponse response = client.search(new NaverShoppingSearchRequest(
				"car organizer",
				2,
				1,
				NaverShoppingSort.SIM,
				"used:rental:cbshop"
		));

		assertThat(response.lastBuildDate()).isEqualTo("Thu, 02 Jul 2026 20:00:00 +0900");
		assertThat(response.total()).isEqualTo(1234);
		assertThat(response.start()).isEqualTo(1);
		assertThat(response.display()).isEqualTo(2);
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().title()).isEqualTo("<b>Car Organizer</b>");
		assertThat(response.items().getFirst().link()).isEqualTo("https://example.com/products/1");
		assertThat(response.items().getFirst().image()).isEqualTo("https://example.com/products/1.jpg");
		assertThat(response.items().getFirst().lprice()).isEqualTo("12900");
		assertThat(response.items().getFirst().hprice()).isEqualTo("15900");
		assertThat(response.items().getFirst().mallName()).isEqualTo("Test Mall");
		assertThat(response.items().getFirst().productId()).isEqualTo("1000001");
		assertThat(response.items().getFirst().productType()).isEqualTo("1");
		assertThat(response.items().getFirst().brand()).isEqualTo("Test Brand");
		assertThat(response.items().getFirst().maker()).isEqualTo("Test Maker");
		assertThat(response.items().getFirst().category1()).isEqualTo("Car");
		assertThat(response.items().getFirst().category2()).isEqualTo("Storage");
		assertThat(response.items().getFirst().category3()).isEqualTo("Interior");
		assertThat(response.items().getFirst().category4()).isEqualTo("Organizer");

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getPath()).startsWith("/v1/search/shop.json?");
		assertThat(request.getHeader("X-Naver-Client-Id")).isEqualTo(CLIENT_ID);
		assertThat(request.getHeader("X-Naver-Client-Secret")).isEqualTo(CLIENT_SECRET);
		HttpUrl requestUrl = request.getRequestUrl();
		assertThat(requestUrl.queryParameter("query")).isEqualTo("car organizer");
		assertThat(requestUrl.queryParameter("display")).isEqualTo("2");
		assertThat(requestUrl.queryParameter("start")).isEqualTo("1");
		assertThat(requestUrl.queryParameter("sort")).isEqualTo("sim");
		assertThat(requestUrl.queryParameter("exclude")).isEqualTo("used:rental:cbshop");
	}

	@Test
	void searchUsesDefaultSortAndOmitsBlankExclude() throws InterruptedException {
		mockWebServer.enqueue(successResponse());

		client.search(new NaverShoppingSearchRequest(
				"usb cable",
				10,
				1,
				null,
				" "
		));

		HttpUrl requestUrl = mockWebServer.takeRequest().getRequestUrl();
		assertThat(requestUrl.queryParameter("sort")).isEqualTo("sim");
		assertThat(requestUrl.queryParameter("exclude")).isNull();
	}

	@Test
	void searchMapsRateLimitResponse() {
		mockWebServer.enqueue(jsonResponse(429, "{}"));

		assertThatThrownBy(() -> client.search(validRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));
	}

	@Test
	void searchMapsClientErrorResponseToExternalApiUnavailable() {
		mockWebServer.enqueue(jsonResponse(400, "{\"errorCode\":\"SE01\",\"errorMessage\":\"invalid query\"}"));

		assertThatThrownBy(() -> client.search(validRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE));
	}

	@Test
	void searchMapsServerErrorResponseToExternalApiUnavailable() {
		mockWebServer.enqueue(jsonResponse(500, "{\"error\":\"temporary failure\"}"));

		assertThatThrownBy(() -> client.search(validRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE));
	}

	@Test
	void searchRejectsMissingCredentialsBeforeRequest() {
		NaverShoppingClient clientWithoutCredentials = new NaverShoppingClient(
				RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build(),
				new NaverApiProperties("", "", 1000)
		);

		assertThatThrownBy(() -> clientWithoutCredentials.search(validRequest()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("NAVER_CLIENT_ID");
		assertThat(mockWebServer.getRequestCount()).isZero();
	}

	private NaverShoppingSearchRequest validRequest() {
		return new NaverShoppingSearchRequest(
				"car organizer",
				10,
				1,
				NaverShoppingSort.SIM,
				null
		);
	}

	private MockResponse successResponse() {
		return jsonResponse(200, """
				{
				  "lastBuildDate": "Thu, 02 Jul 2026 20:00:00 +0900",
				  "total": 1234,
				  "start": 1,
				  "display": 2,
				  "items": [
				    {
				      "title": "<b>Car Organizer</b>",
				      "link": "https://example.com/products/1",
				      "image": "https://example.com/products/1.jpg",
				      "lprice": "12900",
				      "hprice": "15900",
				      "mallName": "Test Mall",
				      "productId": "1000001",
				      "productType": "1",
				      "brand": "Test Brand",
				      "maker": "Test Maker",
				      "category1": "Car",
				      "category2": "Storage",
				      "category3": "Interior",
				      "category4": "Organizer"
				    }
				  ]
				}
				""");
	}

	private MockResponse jsonResponse(int statusCode, String body) {
		return new MockResponse()
				.setResponseCode(statusCode)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody(body);
	}
}
