package com.sellerradar.shopping.client;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

@Component
public class NaverShoppingClient {
	private static final String NAVER_OPEN_API_BASE_URL = "https://openapi.naver.com";
	private static final String SHOPPING_SEARCH_PATH = "/v1/search/shop.json";

	private final RestClient restClient;
	private final NaverApiProperties properties;

	@Autowired
	public NaverShoppingClient(NaverApiProperties properties) {
		this(RestClient.builder().baseUrl(NAVER_OPEN_API_BASE_URL).build(), properties);
	}

	NaverShoppingClient(RestClient restClient, NaverApiProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public NaverShoppingSearchResponse search(NaverShoppingSearchRequest request) {
		validateCredentials();
		return restClient.get()
				.uri(uriBuilder -> buildSearchUri(uriBuilder, request))
				.header("X-Naver-Client-Id", properties.clientId())
				.header("X-Naver-Client-Secret", properties.clientSecret())
				.retrieve()
				.onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
						(requestSpec, response) -> {
							throw new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT);
						})
				.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
						(requestSpec, response) -> {
							throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
						})
				.body(NaverShoppingSearchResponse.class);
	}

	private java.net.URI buildSearchUri(UriBuilder uriBuilder, NaverShoppingSearchRequest request) {
		UriBuilder builder = uriBuilder.path(SHOPPING_SEARCH_PATH)
				.queryParam("query", request.query())
				.queryParam("display", request.display())
				.queryParam("start", request.start())
				.queryParam("sort", request.sort().value());
		if (StringUtils.hasText(request.exclude())) {
			builder.queryParam("exclude", request.exclude());
		}
		return builder.build();
	}

	private void validateCredentials() {
		if (!StringUtils.hasText(properties.clientId()) || !StringUtils.hasText(properties.clientSecret())) {
			throw new IllegalStateException("NAVER_CLIENT_ID와 NAVER_CLIENT_SECRET 환경변수가 필요합니다.");
		}
	}
}
