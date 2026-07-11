package com.sellerradar.trend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiHubProperties;
import com.sellerradar.common.external.config.NaverApiProperties;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.service.ApiQuotaService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class NaverApiHubDataLabClientTest {
	private MockWebServer server;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void explicitEndpointUsesHubHeadersMinimalPayloadAndQuotaGuard() throws Exception {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("""
						{
						  "startDate":"2026-07-01",
						  "endDate":"2026-07-02",
						  "timeUnit":"date",
						  "results":[{
						    "title":"desk",
						    "keyword":["desk"],
						    "data":[{"period":"2026-07-01","ratio":42.5}]
						  }]
						}
						"""));
		ApiQuotaService quotaService = mock(ApiQuotaService.class);
		NaverApiHubDataLabClient client = new NaverApiHubDataLabClient(
				RestClient.builder().build(),
				new NaverApiHubProperties(
						"hub-id",
						"hub-secret",
						"",
						server.url("/configured/insight").toString()
				),
				new NaverApiProperties("legacy-id", "legacy-secret", 1000),
				quotaService
		);

		NaverDataLabKeywordTrendResponse response = client.searchKeywordTrend(new NaverDataLabKeywordTrendRequest(
				LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 2),
				NaverDataLabTimeUnit.DATE,
				"50000000",
				"desk"
		));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/configured/insight");
		assertThat(request.getHeader("X-NCP-APIGW-API-KEY-ID")).isEqualTo("hub-id");
		assertThat(request.getHeader("X-NCP-APIGW-API-KEY")).isEqualTo("hub-secret");
		assertThat(request.getHeader("X-Naver-Client-Id")).isNull();
		String requestBody = request.getBody().readUtf8();
		assertThat(requestBody)
				.contains("\"startDate\":\"2026-07-01\"")
				.contains("\"category\":\"50000000\"")
				.contains("\"keyword\":[{\"name\":\"desk\",\"param\":[\"desk\"]}]")
				.doesNotContain("\"device\"")
				.doesNotContain("\"gender\"")
				.doesNotContain("\"ages\"");
		assertThat(response.results()).hasSize(1);
		assertThat(response.results().getFirst().data()).containsExactly(
				new NaverDataLabTrendPoint("2026-07-01", new BigDecimal("42.5"))
		);
		verify(quotaService).assertDailyQuotaAvailable(
				ExternalApiProvider.NAVER_DATALAB,
				NaverDataLabClient.KEYWORD_TREND_API_NAME,
				1000
		);
	}

	@Test
	void nullCollectionsFromHubAreNormalizedToEmptyLists() {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("""
						{
						  "startDate":"2026-07-01",
						  "endDate":"2026-07-02",
						  "timeUnit":"date",
						  "results":[{"title":"desk","keyword":null,"data":null}]
						}
						"""));

		NaverDataLabKeywordTrendResponse response = client(mock(ApiQuotaService.class))
				.searchKeywordTrend(request());

		assertThat(response.results()).hasSize(1);
		assertThat(response.results().getFirst().keyword()).isEmpty();
		assertThat(response.results().getFirst().data()).isEmpty();
	}

	@Test
	void unauthorizedResponseMapsToUnavailable() {
		server.enqueue(new MockResponse().setResponseCode(401));

		assertThatThrownBy(() -> client(mock(ApiQuotaService.class)).searchKeywordTrend(request()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE));
	}

	@Test
	void rateLimitResponseMapsToRateLimit() {
		server.enqueue(new MockResponse().setResponseCode(429));

		assertThatThrownBy(() -> client(mock(ApiQuotaService.class)).searchKeywordTrend(request()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));
	}

	private NaverApiHubDataLabClient client(ApiQuotaService quotaService) {
		return new NaverApiHubDataLabClient(
				RestClient.builder().build(),
				new NaverApiHubProperties(
						"hub-id",
						"hub-secret",
						"",
						server.url("/configured/insight").toString()
				),
				new NaverApiProperties("legacy-id", "legacy-secret", 1000),
				quotaService
		);
	}

	private NaverDataLabKeywordTrendRequest request() {
		return new NaverDataLabKeywordTrendRequest(
				LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 2),
				NaverDataLabTimeUnit.DATE,
				"50000000",
				"desk"
		);
	}
}
