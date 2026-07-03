package com.sellerradar.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.category.domain.RiskHandlingType;
import com.sellerradar.category.service.RiskCategoryDecision;
import org.junit.jupiter.api.Test;

class ScoringEngineTest {
	private final ScoringEngine scoringEngine = new ScoringEngine();

	@Test
	void calculateReturnsRecommendedForStrongCandidate() {
		ScoringResult result = scoringEngine.calculate(new ScoringInput(
				30,
				8_000L,
				12_000,
				16_000,
				14_000,
				19_900,
				8_000,
				2_500,
				RiskCategoryDecision.safe()
		));

		assertThat(result.breakdown().trendScore()).isEqualTo(30);
		assertThat(result.breakdown().competitionScore()).isEqualTo(25);
		assertThat(result.breakdown().marginScore()).isEqualTo(30);
		assertThat(result.breakdown().priceBandScore()).isEqualTo(10);
		assertThat(result.breakdown().supplyScore()).isEqualTo(5);
		assertThat(result.breakdown().riskPenalty()).isZero();
		assertThat(result.overallScore()).isEqualTo(100);
		assertThat(result.grade()).isEqualTo(CandidateGrade.RECOMMENDED);
		assertThat(result.reasons()).contains(
				"검색 클릭 추이 상승 폭이 큽니다.",
				"예상 마진율이 검토 기준을 충족합니다.",
				"위험 카테고리 룰에 걸리지 않았습니다."
		);
		assertThat(result.warnings()).contains(
				"데이터 기반 검토 후보이며 판매나 수익을 보장하지 않습니다.",
				"데이터랩 ratio는 검색 클릭 추이 기반이며 실제 판매량이 아닙니다."
		);
	}

	@Test
	void calculateClampsComponentScoresAndOverallScore() {
		ScoringResult result = scoringEngine.calculate(new ScoringInput(
				99,
				1L,
				10_000,
				11_000,
				10_500,
				49_900,
				1_000,
				0,
				RiskCategoryDecision.safe()
		));

		assertThat(result.breakdown().trendScore()).isEqualTo(30);
		assertThat(result.overallScore()).isEqualTo(100);
	}

	@Test
	void calculatePenalizesHighCompetitionAndWeakMargin() {
		ScoringResult result = scoringEngine.calculate(new ScoringInput(
				5,
				500_000L,
				2_000,
				90_000,
				20_000,
				120_000,
				118_000,
				4_000,
				RiskCategoryDecision.safe()
		));

		assertThat(result.breakdown().competitionScore()).isZero();
		assertThat(result.breakdown().marginScore()).isZero();
		assertThat(result.breakdown().priceBandScore()).isZero();
		assertThat(result.overallScore()).isEqualTo(10);
		assertThat(result.grade()).isEqualTo(CandidateGrade.HOLD);
		assertThat(result.warnings()).contains(
				"검색 결과 수 기준 경쟁 강도가 높을 수 있습니다.",
				"예상 마진율이 낮아 원가와 배송비 확인이 필요합니다.",
				"초보 셀러 적정 가격대 범위를 벗어났습니다."
		);
	}

	@Test
	void calculateAppliesCautionRiskPenalty() {
		ScoringResult result = scoringEngine.calculate(new ScoringInput(
				25,
				9_000L,
				12_000,
				16_000,
				14_000,
				19_900,
				8_000,
				2_500,
				RiskCategoryDecision.matched(RiskHandlingType.CAUTION, "의류", "사이즈/반품 리스크")
		));

		assertThat(result.breakdown().riskPenalty()).isEqualTo(-15);
		assertThat(result.grade()).isEqualTo(CandidateGrade.RECOMMENDED);
		assertThat(result.warnings()).contains("위험 카테고리 주의: 사이즈/반품 리스크");
	}

	@Test
	void calculateForcesExcludedGradeForExcludedRiskCategory() {
		ScoringResult result = scoringEngine.calculate(new ScoringInput(
				30,
				5_000L,
				12_000,
				16_000,
				14_000,
				19_900,
				8_000,
				2_500,
				RiskCategoryDecision.matched(RiskHandlingType.EXCLUDE, "의료기기", "허가/광고 리스크")
		));

		assertThat(result.breakdown().riskPenalty()).isEqualTo(-40);
		assertThat(result.overallScore()).isEqualTo(60);
		assertThat(result.grade()).isEqualTo(CandidateGrade.EXCLUDED);
		assertThat(result.warnings()).contains("위험 카테고리 제외: 허가/광고 리스크");
	}
}
