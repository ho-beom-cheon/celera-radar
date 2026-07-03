package com.sellerradar.trend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiProperties;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.service.ApiQuotaService;
import java.io.IOException;
import java.time.LocalDate;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class NaverDataLabClientTest {
	private static final String CLIENT_ID = "test-client-id";
	private static final String CLIENT_SECRET = "test-client-secret";
	private static final int DAILY_QUOTA = 1000;

	private MockWebServer mockWebServer;
	private ApiQuotaService apiQuotaService;
	private NaverDataLabClient client;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
		apiQuotaService = mock(ApiQuotaService.class);
		objectMapper = new ObjectMapper();
		RestClient restClient = RestClient.builder()
				.baseUrl(mockWebServer.url("/").toString())
				.build();
		client = new NaverDataLabClient(
				restClient,
				new NaverApiProperties(CLIENT_ID, CLIENT_SECRET, DAILY_QUOTA),
				apiQuotaService
		);
	}

	@AfterEach
	void tearDown() throws IOException {
		mockWebServer.shutdown();
	}

	@Test
	void searchKeywordTrendSendsHeadersAndOfficialKeywordPayload() throws Exception {
		mockWebServer.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("""
						{
						  "startDate": "2026-06-02",
						  "endDate": "2026-07-01",
						  "timeUnit": "date",
						  "results": [
						    {
						      "title": "차량용 수납함",
						      "keyword": ["차량용 수납함"],
						      "data": [
						        {"period": "2026-06-30", "ratio": 82.3},
						        {"period": "2026-07-01", "ratio": 91.4}
						      ]
						    }
						  ]
						}
						"""));

		NaverDataLabKeywordTrendResponse response = client.searchKeywordTrend(
				new NaverDataLabKeywordTrendRequest(
						LocalDate.of(2026, 6, 2),
						LocalDate.of(2026, 7, 1),
						NaverDataLabTimeUnit.DATE,
						"50000000",
						"차량용 수납함"
				)
		);

		assertThat(response.startDate()).isEqualTo("2026-06-02");
		assertThat(response.results()).hasSize(1);
		assertThat(response.results().getFirst().keyword()).containsExactly("차량용 수납함");
		assertThat(response.results().getFirst().data().getLast().ratio()).isEqualByComparingTo("91.4");

		RecordedRequest request = mockWebServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/v1/datalab/shopping/category/keywords");
		assertThat(request.getHeader("X-Naver-Client-Id")).isEqualTo(CLIENT_ID);
		assertThat(request.getHeader("X-Naver-Client-Secret")).isEqualTo(CLIENT_SECRET);
		assertThat(request.getHeader("Content-Type")).startsWith(MediaType.APPLICATION_JSON_VALUE);
		JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
		assertThat(body.get("startDate").asText()).isEqualTo("2026-06-02");
		assertThat(body.get("endDate").asText()).isEqualTo("2026-07-01");
		assertThat(body.get("timeUnit").asText()).isEqualTo("date");
		assertThat(body.get("category").asText()).isEqualTo("50000000");
		assertThat(body.get("keyword").get(0).get("name").asText()).isEqualTo("차량용 수납함");
		assertThat(body.get("keyword").get(0).get("param").get(0).asText()).isEqualTo("차량용 수납함");
		assertThat(body.get("device").asText()).isEmpty();
		assertThat(body.get("gender").asText()).isEmpty();
		assertThat(body.get("ages").size()).isZero();
		verify(apiQuotaService).assertDailyQuotaAvailable(
				ExternalApiProvider.NAVER,
				NaverDataLabClient.KEYWORD_TREND_API_NAME,
				DAILY_QUOTA
		);
	}

	@Test
	void searchKeywordTrendMapsRateLimitResponse() {
		mockWebServer.enqueue(new MockResponse()
				.setResponseCode(429)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("{}"));

		assertThatThrownBy(() -> client.searchKeywordTrend(validRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));
	}

	@Test
	void searchKeywordTrendRejectsWhenLocalQuotaIsExhaustedBeforeRequest() {
		org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT))
				.when(apiQuotaService)
				.assertDailyQuotaAvailable(
						ExternalApiProvider.NAVER,
						NaverDataLabClient.KEYWORD_TREND_API_NAME,
						DAILY_QUOTA
				);

		assertThatThrownBy(() -> client.searchKeywordTrend(validRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));
		assertThat(mockWebServer.getRequestCount()).isZero();
	}

	@Test
	void searchKeywordTrendRejectsMissingCredentialsBeforeQuotaAndRequest() {
		NaverDataLabClient clientWithoutCredentials = new NaverDataLabClient(
				RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build(),
				new NaverApiProperties("", "", DAILY_QUOTA),
				apiQuotaService
		);

		assertThatThrownBy(() -> clientWithoutCredentials.searchKeywordTrend(validRequest()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("NAVER_CLIENT_ID");
		verifyNoInteractions(apiQuotaService);
		assertThat(mockWebServer.getRequestCount()).isZero();
	}

	private NaverDataLabKeywordTrendRequest validRequest() {
		return new NaverDataLabKeywordTrendRequest(
				LocalDate.of(2026, 6, 2),
				LocalDate.of(2026, 7, 1),
				NaverDataLabTimeUnit.DATE,
				"50000000",
				"차량용 수납함"
		);
	}
}
