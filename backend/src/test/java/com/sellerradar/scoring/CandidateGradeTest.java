package com.sellerradar.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CandidateGradeTest {
	@Test
	void fromScoreUsesConfiguredBoundaries() {
		assertThat(CandidateGrade.fromScore(100)).isEqualTo(CandidateGrade.RECOMMENDED);
		assertThat(CandidateGrade.fromScore(80)).isEqualTo(CandidateGrade.RECOMMENDED);
		assertThat(CandidateGrade.fromScore(79)).isEqualTo(CandidateGrade.REVIEW);
		assertThat(CandidateGrade.fromScore(65)).isEqualTo(CandidateGrade.REVIEW);
		assertThat(CandidateGrade.fromScore(64)).isEqualTo(CandidateGrade.HOLD);
		assertThat(CandidateGrade.fromScore(50)).isEqualTo(CandidateGrade.HOLD);
		assertThat(CandidateGrade.fromScore(49)).isEqualTo(CandidateGrade.HOLD);
		assertThat(CandidateGrade.fromScore(0)).isEqualTo(CandidateGrade.HOLD);
	}
}
