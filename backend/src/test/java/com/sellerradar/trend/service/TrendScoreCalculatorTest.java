package com.sellerradar.trend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendScoreCalculatorTest {
	private final TrendScoreCalculator calculator = new TrendScoreCalculator();

	@Test
	void calculateCapsTrendScoreAtThirty() {
		TrendScoreResult result = calculator.calculate(List.of(
				point("2026-06-01", "0"),
				point("2026-06-24", "0"),
				point("2026-07-01", "100")
		));

		assertThat(result.trendDelta7d()).isEqualTo(100.0);
		assertThat(result.trendDelta30d()).isEqualTo(100.0);
		assertThat(result.trendScore()).isEqualTo(30);
		assertThat(result.warnings()).containsExactly(TrendScoreCalculator.DATALAB_SALES_WARNING);
	}

	@Test
	void calculateReturnsZeroForDecliningTrend() {
		TrendScoreResult result = calculator.calculate(List.of(
				point("2026-06-01", "90"),
				point("2026-06-24", "80"),
				point("2026-07-01", "20")
		));

		assertThat(result.trendDelta7d()).isEqualTo(-60.0);
		assertThat(result.trendDelta30d()).isEqualTo(-70.0);
		assertThat(result.trendScore()).isZero();
	}

	@Test
	void calculateHandlesEmptyPointsWithWarning() {
		TrendScoreResult result = calculator.calculate(List.of());

		assertThat(result.trendDelta7d()).isZero();
		assertThat(result.trendDelta30d()).isZero();
		assertThat(result.trendScore()).isZero();
		assertThat(result.warnings()).containsExactly(TrendScoreCalculator.DATALAB_SALES_WARNING);
	}

	@Test
	void calculateUsesNearestEarlierBaselineWhenExactDateIsMissing() {
		TrendScoreResult result = calculator.calculate(List.of(
				point("2026-06-20", "30"),
				point("2026-06-25", "60"),
				point("2026-07-01", "90")
		));

		assertThat(result.trendDelta7d()).isEqualTo(60.0);
		assertThat(result.trendDelta30d()).isEqualTo(60.0);
		assertThat(result.trendScore()).isEqualTo(18);
	}

	private TrendPoint point(String period, String ratio) {
		return new TrendPoint(LocalDate.parse(period), new BigDecimal(ratio));
	}
}
