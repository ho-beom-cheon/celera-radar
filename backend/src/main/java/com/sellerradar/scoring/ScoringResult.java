package com.sellerradar.scoring;

import java.util.List;

public record ScoringResult(
		ScoringBreakdown breakdown,
		int overallScore,
		CandidateGrade grade,
		List<String> reasons,
		List<String> warnings
) {
}
