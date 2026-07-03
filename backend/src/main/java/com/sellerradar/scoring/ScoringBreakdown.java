package com.sellerradar.scoring;

public record ScoringBreakdown(
		int trendScore,
		int competitionScore,
		int marginScore,
		int priceBandScore,
		int supplyScore,
		int riskPenalty
) {
	public int priceScore() {
		return priceBandScore;
	}
}
