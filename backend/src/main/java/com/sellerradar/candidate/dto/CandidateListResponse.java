package com.sellerradar.candidate.dto;

import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.scoring.CandidateGrade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CandidateListResponse(
		Long candidateId,
		String name,
		CandidateSourceType source,
		CategoryCode categoryCode,
		int score,
		CandidateGrade grade,
		int expectedSalePrice,
		Integer supplyPrice,
		Integer shippingFee,
		BigDecimal expectedMarginRate,
		RiskLevel riskLevel,
		CandidateStatus status,
		OffsetDateTime createdAt
) {
	public static CandidateListResponse from(ProductCandidate candidate) {
		CandidateScore score = requiredScore(candidate);
		return new CandidateListResponse(
				candidate.getId(),
				candidate.getName(),
				candidate.getSourceType(),
				candidate.getCategoryCode(),
				score.getOverallScore(),
				candidate.getGrade(),
				candidate.getExpectedSalePrice(),
				candidate.getSupplyPrice(),
				candidate.getShippingFee(),
				candidate.getExpectedMarginRate(),
				score.getRiskLevel(),
				candidate.getStatus(),
				candidate.getCreatedAt()
		);
	}

	private static CandidateScore requiredScore(ProductCandidate candidate) {
		CandidateScore score = candidate.getScore();
		if (score == null) {
			throw new IllegalStateException("Candidate score is missing. candidateId=" + candidate.getId());
		}
		return score;
	}
}
