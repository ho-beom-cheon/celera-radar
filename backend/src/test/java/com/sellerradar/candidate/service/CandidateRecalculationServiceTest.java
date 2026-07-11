package com.sellerradar.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.dto.CandidateRecalculationResponse;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.category.service.RiskCategoryService;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.scoring.CandidateGrade;
import com.sellerradar.scoring.CandidateScoreCalculator;
import com.sellerradar.scoring.ScoringBreakdown;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.trend.domain.TrendTimeUnit;
import com.sellerradar.trend.service.TrendScoreResult;
import com.sellerradar.trend.service.TrendSnapshotService;
import com.sellerradar.user.domain.User;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CandidateRecalculationServiceTest {
	@Test
	void recalculatesOwnedActiveCandidateWithLatestTrendScore() {
		ProductCandidateRepository candidateRepository = mock(ProductCandidateRepository.class);
		WholesaleProductRepository wholesaleProductRepository = mock(WholesaleProductRepository.class);
		ShoppingPriceSnapshotRepository shoppingSnapshotRepository = mock(ShoppingPriceSnapshotRepository.class);
		TrendSnapshotService trendSnapshotService = mock(TrendSnapshotService.class);
		RiskCategoryService riskCategoryService = mock(RiskCategoryService.class);
		CandidateRecalculationService service = new CandidateRecalculationService(
				candidateRepository,
				wholesaleProductRepository,
				shoppingSnapshotRepository,
				trendSnapshotService,
				riskCategoryService,
				new CandidateScoreCalculator()
		);
		User user = User.create("seller@example.com", "{bcrypt}hash");
		ReflectionTestUtils.setField(user, "id", 1L);
		Keyword keyword = Keyword.create(user, "원피스", "원피스", CategoryCode.SEASONAL_LIVING, KeywordPriority.MEDIUM);
		ReflectionTestUtils.setField(keyword, "id", 10L);
		ProductCandidate candidate = ProductCandidate.create(
				user,
				keyword,
				CandidateSourceType.CSV,
				"여름 원피스",
				CategoryCode.SEASONAL_LIVING,
				29_900,
				10_000,
				3_000,
				new BigDecimal("56.52"),
				CandidateGrade.REVIEW
		);
		candidate.assignScore(CandidateScore.create(
				new ScoringBreakdown(0, 0, 0, 0, 0, 0),
				0,
				CandidateGrade.HOLD,
				RiskLevel.LOW,
				List.of(),
				List.of()
		));
		when(candidateRepository.findByUserIdAndStatusNot(1L, CandidateStatus.EXCLUDED)).thenReturn(List.of(candidate));
		when(shoppingSnapshotRepository.findFirstByKeyword_IdOrderBySearchDateDesc(10L)).thenReturn(Optional.empty());
		when(trendSnapshotService.calculateSavedTrendScore(10L, TrendTimeUnit.DATE))
				.thenReturn(new TrendScoreResult(20.0, 40.0, 9, List.of()));

		CandidateRecalculationResponse response = service.recalculate(1L);

		assertThat(response.recalculatedCount()).isEqualTo(1);
		assertThat(response.skippedCount()).isZero();
		assertThat(candidate.getScore().getTrendScore()).isEqualTo(9);
		assertThat(candidate.getScore().getOverallScore()).isGreaterThan(9);
	}
}
