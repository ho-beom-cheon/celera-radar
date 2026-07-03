package com.sellerradar.wholesale.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
