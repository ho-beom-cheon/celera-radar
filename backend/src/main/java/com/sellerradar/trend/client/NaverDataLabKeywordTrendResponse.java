package com.sellerradar.trend.client;

import java.util.List;

public record NaverDataLabKeywordTrendResponse(
		String startDate,
		String endDate,
		String timeUnit,
		List<NaverDataLabKeywordTrendResult> results
) {
	public NaverDataLabKeywordTrendResponse {
		results = results == null ? List.of() : List.copyOf(results);
	}
}
