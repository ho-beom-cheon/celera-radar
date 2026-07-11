package com.sellerradar.candidate.dto;

public record CandidateRecalculationResponse(
		int recalculatedCount,
		int skippedCount
) {
}
