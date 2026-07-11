package com.sellerradar.trend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TrendAnalysisResponse(
		LocalDate snapshotDate,
		LocalDate periodStart,
		LocalDate periodEnd,
		BigDecimal latestRatio,
		double trendDelta7d,
		double trendDelta30d,
		int trendScore,
		List<TrendPointResponse> points,
		List<String> warnings
) {
}
