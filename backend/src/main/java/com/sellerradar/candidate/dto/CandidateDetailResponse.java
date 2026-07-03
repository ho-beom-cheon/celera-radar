package com.sellerradar.candidate.dto;

import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.scoring.CandidateGrade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CandidateDetailResponse(
		Long candidateId,
		String name,
		CandidateSourceType source,
		CategoryCode categoryCode,
		CandidateStatus status,
		int score,
		CandidateGrade grade,
		int expectedSalePrice,
		Integer supplyPrice,
		Integer shippingFee,
		BigDecimal expectedMarginRate,
		RiskLevel riskLevel,
		CandidateScoreBreakdownResponse scoreBreakdown,
		List<String> reasons,
		List<String> warnings,
		Long keywordId,
		Long wholesaleProductId,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public static CandidateDetailResponse from(ProductCandidate candidate) {
		CandidateScore score = requiredScore(candidate);
		Keyword keyword = candidate.getKeyword();
		return new CandidateDetailResponse(
				candidate.getId(),
				candidate.getName(),
				candidate.getSourceType(),
				candidate.getCategoryCode(),
				candidate.getStatus(),
				score.getOverallScore(),
				candidate.getGrade(),
				candidate.getExpectedSalePrice(),
				candidate.getSupplyPrice(),
				candidate.getShippingFee(),
				candidate.getExpectedMarginRate(),
				score.getRiskLevel(),
				CandidateScoreBreakdownResponse.from(score.breakdown()),
				score.reasons(),
				score.warnings(),
				keyword == null ? null : keyword.getId(),
				candidate.getWholesaleProductId(),
				candidate.getCreatedAt(),
				candidate.getUpdatedAt()
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
