package com.sellerradar.keyword.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.dto.KeywordAnalysisResponse;
import com.sellerradar.keyword.dto.ShoppingAnalysisResponse;
import com.sellerradar.keyword.dto.ShoppingSnapshotItemResponse;
import com.sellerradar.keyword.dto.ShoppingSnapshotResponse;
import com.sellerradar.keyword.dto.ShoppingTopItemResponse;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.domain.ShoppingTopItem;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.shopping.service.ShoppingSearchSnapshotService;
import com.sellerradar.shopping.service.ShoppingSnapshotCollectResult;
import com.sellerradar.trend.domain.TrendTimeUnit;
import com.sellerradar.trend.dto.TrendAnalysisResponse;
import com.sellerradar.trend.service.TrendSnapshotService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeywordAnalysisService {
	private final KeywordRepository keywordRepository;
	private final ShoppingPriceSnapshotRepository snapshotRepository;
	private final ShoppingSearchSnapshotService shoppingSearchSnapshotService;
	private final TrendSnapshotService trendSnapshotService;
	private final Clock clock;

	@Autowired
	public KeywordAnalysisService(
			KeywordRepository keywordRepository,
			ShoppingPriceSnapshotRepository snapshotRepository,
			ShoppingSearchSnapshotService shoppingSearchSnapshotService,
			TrendSnapshotService trendSnapshotService
	) {
		this(keywordRepository, snapshotRepository, shoppingSearchSnapshotService, trendSnapshotService, Clock.systemDefaultZone());
	}

	KeywordAnalysisService(
			KeywordRepository keywordRepository,
			ShoppingPriceSnapshotRepository snapshotRepository,
			ShoppingSearchSnapshotService shoppingSearchSnapshotService,
			TrendSnapshotService trendSnapshotService,
			Clock clock
	) {
		this.keywordRepository = keywordRepository;
		this.snapshotRepository = snapshotRepository;
		this.shoppingSearchSnapshotService = shoppingSearchSnapshotService;
		this.trendSnapshotService = trendSnapshotService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public KeywordAnalysisResponse getAnalysis(Long userId, Long keywordId) {
		Keyword keyword = findActiveKeyword(userId, keywordId);
		ShoppingAnalysisResponse shopping = snapshotRepository.findFirstByKeyword_IdOrderBySearchDateDesc(keywordId)
				.map(this::toShoppingAnalysisResponse)
				.orElse(null);
		TrendAnalysisResponse trend = trendSnapshotService.getLatestAnalysis(keywordId, TrendTimeUnit.DATE)
				.orElse(null);
		return new KeywordAnalysisResponse(
				keyword.getId(),
				keyword.getKeyword(),
				keyword.getAnalysisStatus(),
				keyword.getLastAnalyzedAt(),
				shopping,
				trend,
				null
		);
	}

	@Transactional
	public ShoppingSnapshotResponse analyzeShopping(Long userId, Long keywordId) {
		Keyword keyword = findActiveKeyword(userId, keywordId);
		LocalDate searchDate = LocalDate.now(clock);
		ShoppingSnapshotCollectResult result = shoppingSearchSnapshotService.collectWithCacheStatus(keyword.getId(), searchDate);
		return toShoppingSnapshotResponse(result.snapshot(), result.cached());
	}

	@Transactional(readOnly = true)
	public ShoppingSnapshotResponse getLatestShoppingSnapshot(Long userId, Long keywordId) {
		findActiveKeyword(userId, keywordId);
		return snapshotRepository.findFirstByKeyword_IdOrderBySearchDateDesc(keywordId)
				.map(snapshot -> toShoppingSnapshotResponse(snapshot, false))
				.orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_READY));
	}

	private Keyword findActiveKeyword(Long userId, Long keywordId) {
		return keywordRepository.findByIdAndUserId(keywordId, userId)
				.filter(foundKeyword -> foundKeyword.getStatus() == KeywordStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
	}

	private ShoppingAnalysisResponse toShoppingAnalysisResponse(ShoppingPriceSnapshot snapshot) {
		return new ShoppingAnalysisResponse(
				snapshot.getBaseDate(),
				snapshot.getTotalResults(),
				snapshot.getMinPrice(),
				snapshot.getMaxPrice(),
				snapshot.getAvgPrice(),
				snapshot.getTopItems().stream()
						.limit(10)
						.map(this::toTopItemResponse)
						.toList()
		);
	}

	private ShoppingSnapshotResponse toShoppingSnapshotResponse(ShoppingPriceSnapshot snapshot, boolean cached) {
		return new ShoppingSnapshotResponse(
				snapshot.getKeyword().getId(),
				snapshot.getKeyword().getKeyword(),
				snapshot.getSearchDate(),
				snapshot.getSortType(),
				cached,
				snapshot.getTotalCount(),
				snapshot.getMinPrice(),
				snapshot.getMaxPrice(),
				snapshot.getAvgPrice(),
				snapshot.getCompetitionLevel(),
				snapshot.getFetchedAt(),
				snapshot.getTopItems().stream()
						.limit(10)
						.map(this::toShoppingSnapshotItemResponse)
						.toList()
		);
	}

	private ShoppingSnapshotItemResponse toShoppingSnapshotItemResponse(ShoppingTopItem item) {
		return new ShoppingSnapshotItemResponse(
				item.getRankNo(),
				item.getTitleClean() == null ? item.getTitleRaw() : item.getTitleClean(),
				item.getProductUrl(),
				item.getImageUrl(),
				item.getLowPrice(),
				item.getMallName(),
				item.getCategory1(),
				item.getCategory2(),
				item.getCategory3(),
				item.getCategory4()
		);
	}

	private ShoppingTopItemResponse toTopItemResponse(ShoppingTopItem item) {
		return new ShoppingTopItemResponse(
				item.getItemRank(),
				item.getTitle(),
				item.getLink(),
				item.getImage(),
				item.getLprice(),
				item.getHprice(),
				item.getMallName(),
				item.getCategory1(),
				item.getCategory2(),
				item.getCategory3(),
				item.getCategory4()
		);
	}
}
