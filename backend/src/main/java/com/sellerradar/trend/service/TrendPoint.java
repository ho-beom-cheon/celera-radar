package com.sellerradar.trend.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPoint(
		LocalDate period,
		BigDecimal ratio
) {
}
