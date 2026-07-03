package com.sellerradar.trend.client;

import java.util.List;

record NaverDataLabKeywordTrendPayload(
		String startDate,
		String endDate,
		String timeUnit,
		String category,
		List<NaverDataLabKeywordGroup> keyword,
		String device,
		String gender,
		List<String> ages
) {
}
