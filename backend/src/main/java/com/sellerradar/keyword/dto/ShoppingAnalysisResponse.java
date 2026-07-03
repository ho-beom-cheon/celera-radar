package com.sellerradar.keyword.dto;

import java.time.LocalDate;
import java.util.List;

public record ShoppingAnalysisResponse(
		LocalDate baseDate,
		long totalResults,
		Integer minPrice,
		Integer maxPrice,
		Integer avgPrice,
		List<ShoppingTopItemResponse> topItems
) {
}
