package com.sellerradar.scoring;

public enum CandidateGrade {
	RECOMMENDED,
	REVIEW,
	HOLD,
	EXCLUDED;

	public static CandidateGrade fromScore(int overallScore) {
		if (overallScore >= 80) {
			return RECOMMENDED;
		}
		if (overallScore >= 65) {
			return REVIEW;
		}
		return HOLD;
	}
}
