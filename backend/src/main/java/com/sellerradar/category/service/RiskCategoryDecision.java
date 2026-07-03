package com.sellerradar.category.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sellerradar.category.domain.RiskHandlingType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiskCategoryDecision(
		boolean risk,
		boolean excluded,
		RiskHandlingType handlingType,
		String matchedKeyword,
		String reason
) {
	public static RiskCategoryDecision safe() {
		return new RiskCategoryDecision(false, false, null, null, null);
	}

	public static RiskCategoryDecision matched(RiskHandlingType handlingType, String matchedKeyword, String reason) {
		return new RiskCategoryDecision(
				true,
				handlingType == RiskHandlingType.EXCLUDE,
				handlingType,
				matchedKeyword,
				reason
		);
	}
}
