package com.sellerradar.wholesale.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.wholesale.service.MarginCalculator.MarginCalculationRequest;
import com.sellerradar.wholesale.service.MarginCalculator.MarginCalculationResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MarginCalculatorTest {
	private final MarginCalculator marginCalculator = new MarginCalculator();

	@Test
	void expectedSalePriceUsesDefaultTargetMarginAndRoundsUpToHundred() {
		assertThat(marginCalculator.expectedSalePrice(4_200, 3_000)).isEqualTo(9_600);
	}

	@Test
	void marginRateUsesSalePriceSupplyPriceAndShippingFee() {
		assertThat(marginCalculator.marginRate(12_900, 4_200, 3_000))
				.isEqualByComparingTo(new BigDecimal("44.19"));
	}

	@Test
	void calculateIncludesFixedCostsAndPlatformFee() {
		MarginCalculationResult result = marginCalculator.calculate(new MarginCalculationRequest(
				20_000,
				8_000,
				3_000,
				new BigDecimal("3.0"),
				1_000,
				500,
				500,
				new BigDecimal("25.0")
		));

		assertThat(result.totalCost()).isEqualTo(13_600);
		assertThat(result.expectedProfit()).isEqualTo(6_400);
		assertThat(result.expectedMarginRate()).isEqualByComparingTo(new BigDecimal("32.00"));
		assertThat(result.recommendedSalePrice()).isEqualTo(18_100);
	}

	@Test
	void calculateHandlesZeroSalePrice() {
		MarginCalculationResult result = marginCalculator.calculate(new MarginCalculationRequest(
				0,
				1_000,
				null,
				new BigDecimal("3.0"),
				null,
				null,
				null,
				new BigDecimal("25.0")
		));

		assertThat(result.totalCost()).isEqualTo(1_000);
		assertThat(result.expectedProfit()).isEqualTo(-1_000);
		assertThat(result.expectedMarginRate()).isEqualByComparingTo(new BigDecimal("0.00"));
		assertThat(result.recommendedSalePrice()).isEqualTo(1_400);
	}

	@Test
	void calculateReturnsZeroRecommendedSalePriceWhenRateLeavesNoRoom() {
		MarginCalculationResult result = marginCalculator.calculate(new MarginCalculationRequest(
				10_000,
				4_000,
				1_000,
				new BigDecimal("30.0"),
				null,
				null,
				null,
				new BigDecimal("70.0")
		));

		assertThat(result.recommendedSalePrice()).isZero();
	}

	@Test
	void calculateTreatsNullAndNegativeCostsAsZero() {
		MarginCalculationResult result = marginCalculator.calculate(new MarginCalculationRequest(
				10_000,
				-1_000,
				null,
				null,
				null,
				null,
				null,
				null
		));

		assertThat(result.totalCost()).isZero();
		assertThat(result.expectedProfit()).isEqualTo(10_000);
		assertThat(result.expectedMarginRate()).isEqualByComparingTo(new BigDecimal("100.00"));
		assertThat(result.recommendedSalePrice()).isZero();
	}
}
