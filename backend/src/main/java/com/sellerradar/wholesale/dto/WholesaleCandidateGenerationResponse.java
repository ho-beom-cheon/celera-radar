package com.sellerradar.wholesale.dto;

public record WholesaleCandidateGenerationResponse(
		Long fileId,
		int generatedCount,
		int skippedCount
) {
}
