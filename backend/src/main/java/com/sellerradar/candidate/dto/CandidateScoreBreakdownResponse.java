package com.sellerradar.candidate.dto;

import com.sellerradar.scoring.ScoringBreakdown;

public record CandidateScoreBreakdownResponse(
		int trendScore,
		int competitionScore,
		int marginScore,
		int priceBandScore,
		int supplyScore,
		int riskPenalty
) {
	public static CandidateScoreBreakdownResponse from(ScoringBreakdown breakdown) {
		return new CandidateScoreBreakdownResponse(
				breakdown.trendScore(),
				breakdown.competitionScore(),
				breakdown.marginScore(),
				breakdown.priceBandScore(),
				breakdown.supplyScore(),
				breakdown.riskPenalty()
		);
	}
}
