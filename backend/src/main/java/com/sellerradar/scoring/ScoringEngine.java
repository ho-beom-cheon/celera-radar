package com.sellerradar.scoring;

import com.sellerradar.category.domain.RiskHandlingType;
import com.sellerradar.category.service.RiskCategoryDecision;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScoringEngine {
	private static final int BEGINNER_PRICE_MIN = 9_900;
	private static final int BEGINNER_PRICE_MAX = 49_900;

	public ScoringResult calculate(ScoringInput input) {
		int trendScore = clamp(input.trendScore(), 0, 30);
		int competitionScore = competitionScore(input);
		int marginScore = marginScore(input.expectedSalePrice(), input.supplyPrice(), input.shippingFee());
		int priceBandScore = priceBandScore(input.expectedSalePrice());
		int supplyScore = supplyScore(input.supplyPrice(), input.shippingFee());
		int riskPenalty = riskPenalty(input.riskDecision());

		int overallScore = clamp(
				trendScore + competitionScore + marginScore + priceBandScore + supplyScore + riskPenalty,
				0,
				100
		);
		CandidateGrade grade = input.riskDecision().excluded()
				? CandidateGrade.EXCLUDED
				: CandidateGrade.fromScore(overallScore);

		return new ScoringResult(
				new ScoringBreakdown(
						trendScore,
						competitionScore,
						marginScore,
						priceBandScore,
						supplyScore,
						riskPenalty
				),
				overallScore,
				grade,
				reasons(input, trendScore, competitionScore, marginScore, priceBandScore),
				warnings(input, competitionScore, marginScore, priceBandScore, supplyScore)
		);
	}

	private int competitionScore(ScoringInput input) {
		long totalResults = Math.max(0L, input.totalResults());
		int score;
		if (totalResults == 0) {
			score = 0;
		} else if (totalResults <= 10_000) {
			score = 25;
		} else if (totalResults <= 50_000) {
			score = 20;
		} else if (totalResults <= 100_000) {
			score = 15;
		} else if (totalResults <= 300_000) {
			score = 10;
		} else {
			score = 5;
		}
		Integer minPrice = input.minPrice();
		Integer maxPrice = input.maxPrice();
		Integer avgPrice = input.avgPrice();
		if (minPrice != null && maxPrice != null && avgPrice != null && avgPrice > 0) {
			double spreadRatio = (double) (maxPrice - minPrice) / avgPrice;
			if (spreadRatio > 1.0) {
				score -= 5;
			} else if (spreadRatio > 0.5) {
				score -= 2;
			}
		}
		return clamp(score, 0, 25);
	}

	private int marginScore(Integer expectedSalePrice, Integer supplyPrice, Integer shippingFee) {
		double marginRate = marginRate(expectedSalePrice, supplyPrice, shippingFee);
		if (marginRate >= 35.0) {
			return 30;
		}
		if (marginRate >= 30.0) {
			return 27;
		}
		if (marginRate >= 25.0) {
			return 24;
		}
		if (marginRate >= 20.0) {
			return 18;
		}
		if (marginRate >= 15.0) {
			return 12;
		}
		if (marginRate > 0.0) {
			return 6;
		}
		return 0;
	}

	private int priceBandScore(Integer expectedSalePrice) {
		if (expectedSalePrice == null || expectedSalePrice <= 0) {
			return 0;
		}
		if (expectedSalePrice >= BEGINNER_PRICE_MIN && expectedSalePrice <= BEGINNER_PRICE_MAX) {
			return 10;
		}
		if (expectedSalePrice >= 5_000 && expectedSalePrice < BEGINNER_PRICE_MIN) {
			return 5;
		}
		if (expectedSalePrice > BEGINNER_PRICE_MAX && expectedSalePrice <= 79_000) {
			return 5;
		}
		return 0;
	}

	private int supplyScore(Integer supplyPrice, Integer shippingFee) {
		if (supplyPrice == null || supplyPrice <= 0) {
			return 0;
		}
		if (shippingFee == null) {
			return 3;
		}
		return shippingFee >= 0 ? 5 : 0;
	}

	private int riskPenalty(RiskCategoryDecision riskDecision) {
		if (!riskDecision.risk()) {
			return 0;
		}
		if (riskDecision.handlingType() == RiskHandlingType.EXCLUDE) {
			return -40;
		}
		if (riskDecision.handlingType() == RiskHandlingType.CAUTION) {
			return -15;
		}
		return 0;
	}

	private List<String> reasons(
			ScoringInput input,
			int trendScore,
			int competitionScore,
			int marginScore,
			int priceBandScore
	) {
		List<String> reasons = new ArrayList<>();
		if (trendScore >= 20) {
			reasons.add("검색 클릭 추이 상승 폭이 큽니다.");
		}
		if (competitionScore >= 20) {
			reasons.add("검색 결과 수 기준 경쟁 강도가 낮은 편입니다.");
		}
		if (marginScore >= 24) {
			reasons.add("예상 마진율이 검토 기준을 충족합니다.");
		}
		if (priceBandScore == 10) {
			reasons.add("초보 셀러가 검토하기 쉬운 가격대입니다.");
		}
		if (!input.riskDecision().risk()) {
			reasons.add("위험 카테고리 룰에 걸리지 않았습니다.");
		}
		return reasons;
	}

	private List<String> warnings(
			ScoringInput input,
			int competitionScore,
			int marginScore,
			int priceBandScore,
			int supplyScore
	) {
		List<String> warnings = new ArrayList<>();
		warnings.add("데이터 기반 검토 후보이며 판매나 수익을 보장하지 않습니다.");
		if (input.trendScore() > 0) {
			warnings.add("데이터랩 ratio는 검색 클릭 추이 기반이며 실제 판매량이 아닙니다.");
		}
		if (competitionScore <= 10) {
			warnings.add("검색 결과 수 기준 경쟁 강도가 높을 수 있습니다.");
		}
		if (marginScore < 12) {
			warnings.add("예상 마진율이 낮아 원가와 배송비 확인이 필요합니다.");
		}
		if (priceBandScore == 0) {
			warnings.add("초보 셀러 적정 가격대 범위를 벗어났습니다.");
		}
		if (supplyScore < 5) {
			warnings.add("공급가 또는 배송비 정보 확인이 필요합니다.");
		}
		if (input.riskDecision().risk()) {
			String prefix = input.riskDecision().excluded() ? "위험 카테고리 제외" : "위험 카테고리 주의";
			warnings.add(prefix + ": " + input.riskDecision().reason());
		}
		return warnings;
	}

	private double marginRate(Integer expectedSalePrice, Integer supplyPrice, Integer shippingFee) {
		if (expectedSalePrice == null || expectedSalePrice <= 0 || supplyPrice == null || supplyPrice < 0) {
			return 0.0;
		}
		int safeShippingFee = shippingFee == null ? 0 : shippingFee;
		int marginAmount = expectedSalePrice - supplyPrice - safeShippingFee;
		return ((double) marginAmount / expectedSalePrice) * 100.0;
	}

	private int clamp(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}
}
