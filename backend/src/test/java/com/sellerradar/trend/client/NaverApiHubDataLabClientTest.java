package com.sellerradar.trend.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sellerradar.common.external.config.NaverApiHubProperties;
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
	void explicitEndpointUsesHubHeadersAndQuotaGuard() throws Exception {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("""
						{"startDate":"2026-07-01","endDate":"2026-07-02","timeUnit":"date","results":[]}
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

		client.searchKeywordTrend(new NaverDataLabKeywordTrendRequest(
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
		verify(quotaService).assertDailyQuotaAvailable(
				ExternalApiProvider.NAVER_DATALAB,
				NaverDataLabClient.KEYWORD_TREND_API_NAME,
				1000
		);
	}
}
