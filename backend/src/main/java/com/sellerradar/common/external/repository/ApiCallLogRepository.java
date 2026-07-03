package com.sellerradar.common.external.repository;

import com.sellerradar.common.external.domain.ApiCallLog;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {
	long countByProviderAndEndpointAndCalledAtGreaterThanEqualAndCalledAtLessThan(
			ExternalApiProvider provider,
			String endpoint,
			OffsetDateTime startInclusive,
			OffsetDateTime endExclusive
	);

	default long countByProviderAndApiNameAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			ExternalApiProvider provider,
			String apiName,
			OffsetDateTime startInclusive,
			OffsetDateTime endExclusive
	) {
		return countByProviderAndEndpointAndCalledAtGreaterThanEqualAndCalledAtLessThan(
				provider,
				apiName,
				startInclusive,
				endExclusive
		);
	}
}
