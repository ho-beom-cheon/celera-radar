package com.sellerradar.shopping.domain;

public enum ShoppingCompetitionLevel {
	UNKNOWN,
	LOW,
	MEDIUM,
	HIGH,
	VERY_HIGH;

	public static ShoppingCompetitionLevel from(Integer totalCount, int itemCount) {
		if (totalCount == null || itemCount <= 0) {
			return UNKNOWN;
		}
		if (totalCount < 3_000) {
			return LOW;
		}
		if (totalCount < 10_000) {
			return MEDIUM;
		}
		if (totalCount < 50_000) {
			return HIGH;
		}
		return VERY_HIGH;
	}
}
