package com.sellerradar.wholesale.service;

import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.RiskLevel;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.category.service.RiskCategoryDecision;
import com.sellerradar.category.service.RiskCategoryService;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.scoring.ScoringEngine;
import com.sellerradar.scoring.ScoringInput;
import com.sellerradar.scoring.ScoringResult;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.wholesale.domain.WholesaleFile;
import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.domain.WholesaleProductParseStatus;
import com.sellerradar.wholesale.dto.WholesaleCandidateGenerationResponse;
import com.sellerradar.wholesale.repository.WholesaleFileRepository;
import com.sellerradar.wholesale.repository.WholesaleProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WholesaleCandidateGenerationService {
	private final WholesaleFileRepository wholesaleFileRepository;
	private final WholesaleProductRepository wholesaleProductRepository;
	private final ProductCandidateRepository candidateRepository;
	private final KeywordRepository keywordRepository;
	private final ShoppingPriceSnapshotRepository snapshotRepository;
	private final RiskCategoryService riskCategoryService;
	private final ScoringEngine scoringEngine;
	private final MarginCalculator marginCalculator;
	private final CategoryCodeResolver categoryCodeResolver;

	public WholesaleCandidateGenerationService(
			WholesaleFileRepository wholesaleFileRepository,
			WholesaleProductRepository wholesaleProductRepository,
			ProductCandidateRepository candidateRepository,
			KeywordRepository keywordRepository,
			ShoppingPriceSnapshotRepository snapshotRepository,
			RiskCategoryService riskCategoryService,
			ScoringEngine scoringEngine,
			MarginCalculator marginCalculator,
			CategoryCodeResolver categoryCodeResolver
	) {
		this.wholesaleFileRepository = wholesaleFileRepository;
		this.wholesaleProductRepository = wholesaleProductRepository;
		this.candidateRepository = candidateRepository;
		this.keywordRepository = keywordRepository;
		this.snapshotRepository = snapshotRepository;
		this.riskCategoryService = riskCategoryService;
		this.scoringEngine = scoringEngine;
		this.marginCalculator = marginCalculator;
		this.categoryCodeResolver = categoryCodeResolver;
	}

	@Transactional
	public WholesaleCandidateGenerationResponse generate(Long userId, Long fileId) {
		WholesaleFile file = wholesaleFileRepository.findByIdAndUserId(fileId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WHOLESALE_FILE_NOT_FOUND));
		List<Keyword> keywords = keywordRepository.findByUserIdAndStatus(
				userId,
				KeywordStatus.ACTIVE,
				Pageable.unpaged()
		).getContent();
		List<WholesaleProduct> products = wholesaleProductRepository.findByFileIdAndParseStatusOrderByRowNoAsc(
				file.getId(),
				WholesaleProductParseStatus.PARSED
		);
		int generatedCount = 0;
		int skippedCount = 0;
		for (WholesaleProduct product : products) {
			if (candidateRepository.existsByWholesaleProductId(product.getId())) {
				skippedCount++;
				continue;
			}
			ProductCandidate candidate = toCandidate(file, product, keywords);
			candidateRepository.save(candidate);
			generatedCount++;
		}
		return new WholesaleCandidateGenerationResponse(file.getId(), generatedCount, skippedCount);
	}

	private ProductCandidate toCandidate(
			WholesaleFile file,
			WholesaleProduct product,
			List<Keyword> keywords
	) {
		Optional<Keyword> matchedKeyword = matchKeyword(product, keywords);
		Optional<ShoppingPriceSnapshot> latestSnapshot = matchedKeyword
				.flatMap(keyword -> snapshotRepository.findFirstByKeyword_IdOrderBySearchDateDesc(keyword.getId()));
		int expectedSalePrice = latestSnapshot
				.map(this::snapshotSalePrice)
				.filter(price -> price > 0)
				.orElseGet(() -> marginCalculator.expectedSalePrice(product.getSupplyPrice(), product.getShippingFee()));
		CategoryCode categoryCode = matchedKeyword
				.map(Keyword::getCategoryCode)
				.orElseGet(() -> categoryCodeResolver.resolve(product.getSourceCategory()));
		RiskCategoryDecision riskDecision = riskCategoryService.evaluate(product.getSourceCategory());
		ScoringResult result = scoringEngine.calculate(new ScoringInput(
				0,
				latestSnapshot.map(ShoppingPriceSnapshot::getTotalResults).orElse(0L),
				latestSnapshot.map(ShoppingPriceSnapshot::getMinPrice).orElse(expectedSalePrice),
				latestSnapshot.map(ShoppingPriceSnapshot::getMaxPrice).orElse(expectedSalePrice),
				latestSnapshot.map(ShoppingPriceSnapshot::getAvgPrice).orElse(expectedSalePrice),
				expectedSalePrice,
				product.getSupplyPrice(),
				product.getShippingFee(),
				riskDecision
		));
		ProductCandidate candidate = ProductCandidate.create(
				file.getUser(),
				matchedKeyword.orElse(null),
				CandidateSourceType.CSV,
				product.getProductName(),
				categoryCode,
				expectedSalePrice,
				product.getSupplyPrice(),
				product.getShippingFee(),
				marginCalculator.marginRate(expectedSalePrice, product.getSupplyPrice(), product.getShippingFee()),
				result.grade()
		);
		candidate.linkWholesaleProduct(product.getId());
		candidate.assignScore(CandidateScore.create(
				result.breakdown(),
				result.overallScore(),
				result.grade(),
				riskLevel(riskDecision),
				result.reasons(),
				result.warnings()
		));
		return candidate;
	}

	private Optional<Keyword> matchKeyword(WholesaleProduct product, List<Keyword> keywords) {
		String normalizedName = product.getNormalizedName();
		if (normalizedName == null || normalizedName.isBlank()) {
			return Optional.empty();
		}
		return keywords.stream()
				.filter(keyword -> {
					String normalizedKeyword = keyword.getNormalizedKeyword();
					return normalizedName.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedName);
				})
				.findFirst();
	}

	private int snapshotSalePrice(ShoppingPriceSnapshot snapshot) {
		if (snapshot.getAvgPrice() != null) {
			return snapshot.getAvgPrice();
		}
		if (snapshot.getMinPrice() != null) {
			return snapshot.getMinPrice();
		}
		return 0;
	}

	private RiskLevel riskLevel(RiskCategoryDecision decision) {
		if (decision.excluded()) {
			return RiskLevel.EXCLUDED;
		}
		if (decision.risk()) {
			return RiskLevel.CAUTION;
		}
		return RiskLevel.LOW;
	}
}
