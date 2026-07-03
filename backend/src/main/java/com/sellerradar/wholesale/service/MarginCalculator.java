package com.sellerradar.wholesale.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class MarginCalculator {
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal DEFAULT_TARGET_MARGIN_RATE = new BigDecimal("0.25");

	public MarginCalculationResult calculate(MarginCalculationRequest request) {
		int salePrice = safe(request.salePrice());
		int fixedCost = safe(request.supplyPrice())
				+ safe(request.shippingFee())
				+ safe(request.adCost())
				+ safe(request.couponCost())
				+ safe(request.extraCost());
		BigDecimal platformFeeRate = safeRate(request.platformFeeRate());
		int platformFee = rateCost(salePrice, platformFeeRate);
		int totalCost = fixedCost + platformFee;
		int expectedProfit = salePrice - totalCost;
		return new MarginCalculationResult(
				totalCost,
				expectedProfit,
				marginRate(salePrice, totalCost),
				recommendedSalePrice(fixedCost, platformFeeRate, safeTargetRate(request.targetMarginRate()))
		);
	}

	public int expectedSalePrice(Integer supplyPrice, Integer shippingFee) {
		int totalCost = safe(supplyPrice) + safe(shippingFee);
		if (totalCost <= 0) {
			return 0;
		}
		return recommendedSalePrice(totalCost, BigDecimal.ZERO, DEFAULT_TARGET_MARGIN_RATE);
	}

	public BigDecimal marginRate(int salePrice, Integer supplyPrice, Integer shippingFee) {
		return marginRate(salePrice, safe(supplyPrice) + safe(shippingFee));
	}

	private BigDecimal marginRate(int salePrice, int totalCost) {
		if (salePrice <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		int expectedProfit = salePrice - totalCost;
		return BigDecimal.valueOf(expectedProfit)
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(salePrice), 2, RoundingMode.HALF_UP);
	}

	private int safe(Integer value) {
		return value == null ? 0 : Math.max(value, 0);
	}

	private BigDecimal safeRate(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			return BigDecimal.ZERO;
		}
		return value.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
	}

	private BigDecimal safeTargetRate(BigDecimal value) {
		return value == null ? DEFAULT_TARGET_MARGIN_RATE : safeRate(value);
	}

	private int rateCost(int salePrice, BigDecimal rate) {
		if (salePrice <= 0 || rate.signum() == 0) {
			return 0;
		}
		return BigDecimal.valueOf(salePrice)
				.multiply(rate)
				.setScale(0, RoundingMode.CEILING)
				.intValue();
	}

	private int recommendedSalePrice(int fixedCost, BigDecimal platformFeeRate, BigDecimal targetMarginRate) {
		if (fixedCost <= 0) {
			return 0;
		}
		BigDecimal denominator = BigDecimal.ONE
				.subtract(platformFeeRate)
				.subtract(targetMarginRate);
		if (denominator.signum() <= 0) {
			return 0;
		}
		BigDecimal salePrice = BigDecimal.valueOf(fixedCost)
				.divide(denominator, 0, RoundingMode.CEILING);
		return roundUpToHundred(salePrice.intValue());
	}

	private int roundUpToHundred(int value) {
		return ((value + 99) / 100) * 100;
	}

	public record MarginCalculationRequest(
			Integer salePrice,
			Integer supplyPrice,
			Integer shippingFee,
			BigDecimal platformFeeRate,
			Integer adCost,
			Integer couponCost,
			Integer extraCost,
			BigDecimal targetMarginRate
	) {
	}

	public record MarginCalculationResult(
			int totalCost,
			int expectedProfit,
			BigDecimal expectedMarginRate,
			int recommendedSalePrice
	) {
	}
}
