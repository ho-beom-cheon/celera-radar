package com.sellerradar.wholesale.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class MarginCalculator {
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal DEFAULT_TARGET_MARGIN_RATE = new BigDecimal("0.25");

	public int expectedSalePrice(Integer supplyPrice, Integer shippingFee) {
		int totalCost = safe(supplyPrice) + safe(shippingFee);
		if (totalCost <= 0) {
			return 0;
		}
		BigDecimal salePrice = BigDecimal.valueOf(totalCost)
				.divide(BigDecimal.ONE.subtract(DEFAULT_TARGET_MARGIN_RATE), 0, RoundingMode.CEILING);
		return roundUpToHundred(salePrice.intValue());
	}

	public BigDecimal marginRate(int salePrice, Integer supplyPrice, Integer shippingFee) {
		if (salePrice <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		int margin = salePrice - safe(supplyPrice) - safe(shippingFee);
		return BigDecimal.valueOf(margin)
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(salePrice), 2, RoundingMode.HALF_UP);
	}

	private int safe(Integer value) {
		return value == null ? 0 : value;
	}

	private int roundUpToHundred(int value) {
		return ((value + 99) / 100) * 100;
	}
}
