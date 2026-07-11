package com.sellerradar.shopping.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.common.external.config.NaverApiHubProperties;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class NaverApiHubShoppingClientTest {
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
	void explicitEndpointUsesOnlyHubHeaders() throws Exception {
		server.enqueue(new MockResponse()
				.setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.setBody("""
						{"total":0,"start":1,"display":0,"items":[]}
						"""));
		NaverApiHubShoppingClient client = new NaverApiHubShoppingClient(
				RestClient.builder().build(),
				new NaverApiHubProperties(
						"hub-id",
						"hub-secret",
						server.url("/configured/search").toString(),
						""
				)
		);

		client.search(new NaverShoppingSearchRequest("desk", 10, 1, NaverShoppingSort.SIM, ""));

		RecordedRequest request = server.takeRequest();
		assertThat(request.getPath()).startsWith("/configured/search?");
		assertThat(request.getHeader("X-NCP-APIGW-API-KEY-ID")).isEqualTo("hub-id");
		assertThat(request.getHeader("X-NCP-APIGW-API-KEY")).isEqualTo("hub-secret");
		assertThat(request.getHeader("X-Naver-Client-Id")).isNull();
		assertThat(request.getHeader("X-Naver-Client-Secret")).isNull();
	}
}
