package com.sellerradar.candidate.service;

import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.dto.CandidateRecalculationResponse;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.RiskHandlingType;
import com.sellerradar.category.service.RiskCategoryDecision;
import com.sellerradar.category.service.RiskCategoryService;
import com.sellerradar.scoring.CandidateScoreCalculator;
import com.sellerradar.scoring.ScoringInput;
import com.sellerradar.scoring.ScoringResult;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.trend.domain.TrendTimeUnit;
import com.sellerradar.trend.service.TrendSnapshotService;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateRecalculationService {
	private final ProductCandidateRepository candidateRepository;
	private final WholesaleProductRepository wholesaleProductRepository;
	private final ShoppingPriceSnapshotRepository shoppingSnapshotRepository;
	private final TrendSnapshotService trendSnapshotService;
	private final RiskCategoryService riskCategoryService;
	private final CandidateScoreCalculator candidateScoreCalculator;

	public CandidateRecalculationService(
			ProductCandidateRepository candidateRepository,
			WholesaleProductRepository wholesaleProductRepository,
			ShoppingPriceSnapshotRepository shoppingSnapshotRepository,
			TrendSnapshotService trendSnapshotService,
			RiskCategoryService riskCategoryService,
			CandidateScoreCalculator candidateScoreCalculator
	) {
		this.candidateRepository = candidateRepository;
		this.wholesaleProductRepository = wholesaleProductRepository;
		this.shoppingSnapshotRepository = shoppingSnapshotRepository;
		this.trendSnapshotService = trendSnapshotService;
		this.riskCategoryService = riskCategoryService;
		this.candidateScoreCalculator = candidateScoreCalculator;
	}

	@Transactional
	public CandidateRecalculationResponse recalculate(Long userId) {
		int recalculatedCount = 0;
		int skippedCount = 0;
		for (ProductCandidate candidate : candidateRepository.findByUserIdAndStatusNot(userId, CandidateStatus.EXCLUDED)) {
			if (candidate.getKeyword() == null) {
				skippedCount++;
				continue;
			}
			recalculate(candidate);
			recalculatedCount++;
		}
		return new CandidateRecalculationResponse(recalculatedCount, skippedCount);
	}

	private void recalculate(ProductCandidate candidate) {
		Long keywordId = candidate.getKeyword().getId();
		Optional<ShoppingPriceSnapshot> snapshot = shoppingSnapshotRepository
				.findFirstByKeyword_IdOrderBySearchDateDesc(keywordId);
		int trendScore = trendSnapshotService.calculateSavedTrendScore(keywordId, TrendTimeUnit.DATE).trendScore();
		RiskCategoryDecision riskDecision = resolveRiskDecision(candidate);
		ScoringResult result = candidateScoreCalculator.calculate(new ScoringInput(
				trendScore,
				snapshot.map(ShoppingPriceSnapshot::getTotalResults).orElse(0L),
				snapshot.map(ShoppingPriceSnapshot::getMinPrice).orElse(candidate.getExpectedSalePrice()),
				snapshot.map(ShoppingPriceSnapshot::getMaxPrice).orElse(candidate.getExpectedSalePrice()),
				snapshot.map(ShoppingPriceSnapshot::getAvgPrice).orElse(candidate.getExpectedSalePrice()),
				candidate.getExpectedSalePrice(),
				candidate.getSupplyPrice(),
				candidate.getShippingFee(),
				riskDecision
		));
		candidate.updateScore(
				result.breakdown(),
				result.overallScore(),
				result.grade(),
				riskLevel(riskDecision),
				result.reasons(),
				result.warnings()
		);
	}

	private RiskCategoryDecision resolveRiskDecision(ProductCandidate candidate) {
		if (candidate.getWholesaleProductId() != null) {
			Optional<WholesaleProduct> product = wholesaleProductRepository.findById(candidate.getWholesaleProductId());
			if (product.isPresent()) {
				return riskCategoryService.evaluate(product.get().getSourceCategory());
			}
		}
		CandidateScore score = candidate.getScore();
		if (score == null || score.getRiskLevel() == RiskLevel.LOW) {
			return RiskCategoryDecision.safe();
		}
		RiskHandlingType handlingType = score.getRiskLevel() == RiskLevel.EXCLUDED
				? RiskHandlingType.EXCLUDE
				: RiskHandlingType.CAUTION;
		return RiskCategoryDecision.matched(handlingType, "stored-score", "기존 위험 판정을 유지했습니다.");
	}

	private RiskLevel riskLevel(RiskCategoryDecision decision) {
		if (decision.excluded()) {
			return RiskLevel.EXCLUDED;
		}
		return decision.risk() ? RiskLevel.CAUTION : RiskLevel.LOW;
	}
}
