package com.sellerradar.trend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPointResponse(
		LocalDate period,
		BigDecimal ratio
) {
}
