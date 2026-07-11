package com.sellerradar.trend.client;

import java.util.List;

record NaverApiHubKeywordTrendPayload(
		String startDate,
		String endDate,
		String timeUnit,
		String category,
		List<NaverDataLabKeywordGroup> keyword
) {
}
