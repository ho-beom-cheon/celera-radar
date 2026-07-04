package com.sellerradar.smartstore.settlement.service;

import java.math.BigDecimal;

public record StoreProfitComparisonInput(
		BigDecimal estimatedProfit,
		BigDecimal actualSettlementAmount,
		BigDecimal purchaseCost,
		BigDecimal actualFeeAmount
) {
}
