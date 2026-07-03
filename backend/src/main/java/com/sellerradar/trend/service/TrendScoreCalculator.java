package com.sellerradar.trend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TrendScoreCalculator {
	public static final String DATALAB_SALES_WARNING = "데이터랩 ratio는 검색 클릭 추이 기반이며 실제 판매량이 아닙니다.";

	public TrendScoreResult calculate(List<TrendPoint> points) {
		List<TrendPoint> sortedPoints = points.stream()
				.sorted(Comparator.comparing(TrendPoint::period))
				.toList();
		if (sortedPoints.isEmpty()) {
			return new TrendScoreResult(0.0, 0.0, 0, List.of(DATALAB_SALES_WARNING));
		}

		TrendPoint latest = sortedPoints.getLast();
		double latestRatio = toDouble(latest.ratio());
		double trendDelta7d = latestRatio - baselineRatio(sortedPoints, latest.period().minusDays(7));
		double trendDelta30d = latestRatio - baselineRatio(sortedPoints, latest.period().minusDays(30));
		int trendScore = score(trendDelta7d, trendDelta30d);

		return new TrendScoreResult(
				roundOneDecimal(trendDelta7d),
				roundOneDecimal(trendDelta30d),
				trendScore,
				List.of(DATALAB_SALES_WARNING)
		);
	}

	private double baselineRatio(List<TrendPoint> sortedPoints, LocalDate targetDate) {
		TrendPoint baseline = sortedPoints.stream()
				.filter(point -> !point.period().isAfter(targetDate))
				.reduce((previous, current) -> current)
				.orElse(sortedPoints.getFirst());
		return toDouble(baseline.ratio());
	}

	private int score(double trendDelta7d, double trendDelta30d) {
		double score = positiveClamped(trendDelta7d) * 0.15 + positiveClamped(trendDelta30d) * 0.15;
		return (int) Math.round(Math.min(30.0, score));
	}

	private double positiveClamped(double value) {
		return Math.min(100.0, Math.max(0.0, value));
	}

	private double toDouble(BigDecimal value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private double roundOneDecimal(double value) {
		return Math.round(value * 10.0) / 10.0;
	}
}
