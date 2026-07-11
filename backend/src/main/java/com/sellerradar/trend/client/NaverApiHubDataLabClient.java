package com.sellerradar.trend.client;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiHubProperties;
import com.sellerradar.common.external.config.NaverApiProperties;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.service.ApiQuotaService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverApiHubDataLabClient {
	private final RestClient restClient;
	private final NaverApiHubProperties hubProperties;
	private final NaverApiProperties naverProperties;
	private final ApiQuotaService apiQuotaService;

	@Autowired
	public NaverApiHubDataLabClient(
			NaverApiHubProperties hubProperties,
			NaverApiProperties naverProperties,
			ApiQuotaService apiQuotaService
	) {
		this(RestClient.builder().build(), hubProperties, naverProperties, apiQuotaService);
	}

	NaverApiHubDataLabClient(
			RestClient restClient,
			NaverApiHubProperties hubProperties,
			NaverApiProperties naverProperties,
			ApiQuotaService apiQuotaService
	) {
		this.restClient = restClient;
		this.hubProperties = hubProperties;
		this.naverProperties = naverProperties;
		this.apiQuotaService = apiQuotaService;
	}

	public NaverDataLabKeywordTrendResponse searchKeywordTrend(NaverDataLabKeywordTrendRequest request) {
		validateConfiguration();
		apiQuotaService.assertDailyQuotaAvailable(
				ExternalApiProvider.NAVER_DATALAB,
				NaverDataLabClient.KEYWORD_TREND_API_NAME,
				naverProperties.datalabDailyQuota()
		);
		NaverDataLabKeywordTrendResponse response = restClient.post()
				.uri(hubProperties.shoppingInsightEndpoint())
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-NCP-APIGW-API-KEY-ID", hubProperties.clientId())
				.header("X-NCP-APIGW-API-KEY", hubProperties.clientSecret())
				.body(toPayload(request))
				.retrieve()
				.onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
						(requestSpec, httpResponse) -> {
							throw new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT);
						})
				.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
						(requestSpec, httpResponse) -> {
							throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
						})
				.body(NaverDataLabKeywordTrendResponse.class);
		return Optional.ofNullable(response)
				.orElseThrow(() -> new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE));
	}

	private NaverApiHubKeywordTrendPayload toPayload(NaverDataLabKeywordTrendRequest request) {
		return new NaverApiHubKeywordTrendPayload(
				request.startDate().toString(),
				request.endDate().toString(),
				request.timeUnit().value(),
				request.category(),
				request.keywordGroups()
		);
	}

	private void validateConfiguration() {
		if (!hubProperties.hasCredentials() || !hubProperties.hasShoppingInsightEndpoint()) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
		}
	}
}
