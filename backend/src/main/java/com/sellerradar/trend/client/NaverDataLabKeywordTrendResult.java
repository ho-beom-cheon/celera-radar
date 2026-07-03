package com.sellerradar.trend.client;

import java.util.List;

public record NaverDataLabKeywordTrendResult(
		String title,
		List<String> keyword,
		List<NaverDataLabTrendPoint> data
) {
}
