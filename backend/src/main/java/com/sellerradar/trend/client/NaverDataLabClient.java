package com.sellerradar.trend.client;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.config.NaverApiProperties;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.service.ApiQuotaService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NaverDataLabClient {
	public static final String KEYWORD_TREND_API_NAME = "NAVER_DATALAB_SHOPPING_KEYWORD_TREND";
	private static final String NAVER_OPEN_API_BASE_URL = "https://openapi.naver.com";
	private static final String CATEGORY_KEYWORDS_PATH = "/v1/datalab/shopping/category/keywords";

	private final RestClient restClient;
	private final NaverApiProperties properties;
	private final ApiQuotaService apiQuotaService;

	@Autowired
	public NaverDataLabClient(NaverApiProperties properties, ApiQuotaService apiQuotaService) {
		this(RestClient.builder().baseUrl(NAVER_OPEN_API_BASE_URL).build(), properties, apiQuotaService);
	}

	NaverDataLabClient(
			RestClient restClient,
			NaverApiProperties properties,
			ApiQuotaService apiQuotaService
	) {
		this.restClient = restClient;
		this.properties = properties;
		this.apiQuotaService = apiQuotaService;
	}

	public NaverDataLabKeywordTrendResponse searchKeywordTrend(NaverDataLabKeywordTrendRequest request) {
		validateCredentials();
		apiQuotaService.assertDailyQuotaAvailable(
				ExternalApiProvider.NAVER_DATALAB,
				KEYWORD_TREND_API_NAME,
				properties.datalabDailyQuota()
		);
		return restClient.post()
				.uri(CATEGORY_KEYWORDS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Naver-Client-Id", properties.clientId())
				.header("X-Naver-Client-Secret", properties.clientSecret())
				.body(toPayload(request))
				.retrieve()
				.onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
						(requestSpec, response) -> {
							throw new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT);
						})
				.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
						(requestSpec, response) -> {
							throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
						})
				.body(NaverDataLabKeywordTrendResponse.class);
	}

	private NaverDataLabKeywordTrendPayload toPayload(NaverDataLabKeywordTrendRequest request) {
		return new NaverDataLabKeywordTrendPayload(
				request.startDate().toString(),
				request.endDate().toString(),
				request.timeUnit().value(),
				request.category(),
				List.of(new NaverDataLabKeywordGroup(request.keyword(), List.of(request.keyword()))),
				"",
				"",
				List.of()
		);
	}

	private void validateCredentials() {
		if (!StringUtils.hasText(properties.clientId()) || !StringUtils.hasText(properties.clientSecret())) {
			throw new IllegalStateException("NAVER_CLIENT_ID와 NAVER_CLIENT_SECRET 환경변수가 필요합니다.");
		}
	}
}
