package com.sellerradar.trend.service;

import java.util.List;

public record TrendScoreResult(
		double trendDelta7d,
		double trendDelta30d,
		int trendScore,
		List<String> warnings
) {
}
