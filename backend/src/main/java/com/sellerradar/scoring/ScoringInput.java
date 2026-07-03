package com.sellerradar.scoring;

import com.sellerradar.category.service.RiskCategoryDecision;

public record ScoringInput(
		int trendScore,
		long totalResults,
		Integer minPrice,
		Integer maxPrice,
		Integer avgPrice,
		Integer expectedSalePrice,
		Integer supplyPrice,
		Integer shippingFee,
		RiskCategoryDecision riskDecision
) {
	public ScoringInput {
		if (riskDecision == null) {
			riskDecision = RiskCategoryDecision.safe();
		}
	}
}
