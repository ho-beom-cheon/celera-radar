package com.sellerradar.trend.client;

import java.math.BigDecimal;

public record NaverDataLabTrendPoint(
		String period,
		BigDecimal ratio
) {
}
