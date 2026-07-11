package com.sellerradar.shopping.client;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiHubProperties;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverApiHubShoppingClient {
	private final RestClient restClient;
	private final NaverApiHubProperties properties;

	@Autowired
	public NaverApiHubShoppingClient(NaverApiHubProperties properties) {
		this(RestClient.builder().build(), properties);
	}

	NaverApiHubShoppingClient(RestClient restClient, NaverApiHubProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public NaverShoppingSearchResponse search(NaverShoppingSearchRequest request) {
		validateConfiguration();
		URI uri = UriComponentsBuilder.fromUriString(properties.shoppingSearchEndpoint())
				.queryParam("query", request.query())
				.queryParam("display", request.display())
				.queryParam("start", request.start())
				.queryParam("sort", request.sort().value())
				.queryParamIfPresent("exclude", java.util.Optional.ofNullable(request.exclude())
						.filter(StringUtils::hasText))
				.build()
				.encode()
				.toUri();
		return restClient.get()
				.uri(uri)
				.header("X-NCP-APIGW-API-KEY-ID", properties.clientId())
				.header("X-NCP-APIGW-API-KEY", properties.clientSecret())
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

	private void validateConfiguration() {
		if (!properties.hasCredentials() || !properties.hasShoppingSearchEndpoint()) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
		}
	}
}
