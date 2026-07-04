package com.sellerradar.shopping.service;

import com.sellerradar.shopping.domain.ShoppingCompetitionLevel;
import org.springframework.stereotype.Component;

@Component
public class CompetitionAnalyzer {
	public ShoppingCompetitionLevel analyze(Integer totalCount, int itemCount) {
		if (totalCount == null || itemCount <= 0) {
			return ShoppingCompetitionLevel.UNKNOWN;
		}
		if (totalCount >= 10_000) {
			return ShoppingCompetitionLevel.HIGH;
		}
		if (totalCount >= 3_000) {
			return ShoppingCompetitionLevel.MEDIUM;
		}
		return ShoppingCompetitionLevel.LOW;
	}
}
