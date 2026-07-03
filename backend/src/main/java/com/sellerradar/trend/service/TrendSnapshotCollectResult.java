package com.sellerradar.trend.service;

import java.util.List;

public record TrendSnapshotCollectResult(
		Long keywordId,
		int savedCount,
		TrendScoreResult score,
		List<TrendPoint> points
) {
}
