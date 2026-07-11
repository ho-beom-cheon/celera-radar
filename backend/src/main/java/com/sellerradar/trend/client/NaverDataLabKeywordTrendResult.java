package com.sellerradar.trend.client;

import java.util.List;

public record NaverDataLabKeywordTrendResult(
		String title,
		List<String> keyword,
		List<NaverDataLabTrendPoint> data
) {
	public NaverDataLabKeywordTrendResult {
		keyword = keyword == null ? List.of() : List.copyOf(keyword);
		data = data == null ? List.of() : List.copyOf(data);
	}
}
