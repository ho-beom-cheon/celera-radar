package com.sellerradar.common.external.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiQuotaService {
	private final ApiCallLogRepository apiCallLogRepository;
	private final Clock clock;

	@Autowired
	public ApiQuotaService(ApiCallLogRepository apiCallLogRepository) {
		this(apiCallLogRepository, Clock.systemDefaultZone());
	}

	ApiQuotaService(ApiCallLogRepository apiCallLogRepository, Clock clock) {
		this.apiCallLogRepository = apiCallLogRepository;
		this.clock = clock;
	}

	public void assertDailyQuotaAvailable(ExternalApiProvider provider, String apiName, int dailyLimit) {
		if (remainingDailyQuota(provider, apiName, dailyLimit) <= 0) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT);
		}
	}

	public long remainingDailyQuota(ExternalApiProvider provider, String apiName, int dailyLimit) {
		if (dailyLimit <= 0) {
			return 0;
		}
		long usedCount = todayCallCount(provider, apiName);
		return Math.max(0, dailyLimit - usedCount);
	}

	private long todayCallCount(ExternalApiProvider provider, String apiName) {
		LocalDate today = LocalDate.now(clock);
		OffsetDateTime startInclusive = today.atStartOfDay(clock.getZone()).toOffsetDateTime();
		OffsetDateTime endExclusive = today.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
		return apiCallLogRepository.countByProviderAndApiNameAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
				provider,
				apiName,
				startInclusive,
				endExclusive
		);
	}
}
