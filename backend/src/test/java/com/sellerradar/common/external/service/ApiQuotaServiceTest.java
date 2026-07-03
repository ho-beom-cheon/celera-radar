package com.sellerradar.common.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiQuotaServiceTest {
	private static final String API_NAME = "NAVER_DATALAB_SHOPPING_KEYWORD_TREND";
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T12:00:00Z"), ZoneOffset.UTC);

	private ApiCallLogRepository apiCallLogRepository;
	private ApiQuotaService apiQuotaService;

	@BeforeEach
	void setUp() {
		apiCallLogRepository = mock(ApiCallLogRepository.class);
		apiQuotaService = new ApiQuotaService(apiCallLogRepository, FIXED_CLOCK);
	}

	@Test
	void remainingDailyQuotaCountsTodayApiCallLogs() {
		when(apiCallLogRepository.countByProviderAndEndpointAndCalledAtGreaterThanEqualAndCalledAtLessThan(
				eq(ExternalApiProvider.NAVER_DATALAB),
				eq(API_NAME),
				eq(OffsetDateTime.parse("2026-07-02T00:00:00Z")),
				eq(OffsetDateTime.parse("2026-07-03T00:00:00Z"))
		)).thenReturn(999L);

		long remaining = apiQuotaService.remainingDailyQuota(ExternalApiProvider.NAVER_DATALAB, API_NAME, 1000);

		assertThat(remaining).isEqualTo(1);
		verify(apiCallLogRepository).countByProviderAndEndpointAndCalledAtGreaterThanEqualAndCalledAtLessThan(
				ExternalApiProvider.NAVER_DATALAB,
				API_NAME,
				OffsetDateTime.parse("2026-07-02T00:00:00Z"),
				OffsetDateTime.parse("2026-07-03T00:00:00Z")
		);
	}

	@Test
	void assertDailyQuotaAvailableRejectsWhenQuotaIsExhausted() {
		when(apiCallLogRepository.countByProviderAndEndpointAndCalledAtGreaterThanEqualAndCalledAtLessThan(
				eq(ExternalApiProvider.NAVER_DATALAB),
				eq(API_NAME),
				eq(OffsetDateTime.parse("2026-07-02T00:00:00Z")),
				eq(OffsetDateTime.parse("2026-07-03T00:00:00Z"))
		)).thenReturn(1000L);

		assertThatThrownBy(() ->
				apiQuotaService.assertDailyQuotaAvailable(ExternalApiProvider.NAVER_DATALAB, API_NAME, 1000))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));
	}
}
