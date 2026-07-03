package com.sellerradar.smartstore.settlement.service;

import java.math.BigDecimal;

public record StoreProfitComparison(
		BigDecimal estimatedProfit,
		BigDecimal actualProfit,
		BigDecimal profitGap,
		ProfitComparisonStatus status
) {
}
