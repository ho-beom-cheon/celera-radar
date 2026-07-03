package com.sellerradar.keyword.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.dto.KeywordAnalysisResponse;
import com.sellerradar.keyword.dto.ShoppingAnalysisResponse;
import com.sellerradar.keyword.dto.ShoppingTopItemResponse;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.domain.ShoppingTopItem;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeywordAnalysisService {
	private final KeywordRepository keywordRepository;
	private final ShoppingPriceSnapshotRepository snapshotRepository;

	public KeywordAnalysisService(
			KeywordRepository keywordRepository,
			ShoppingPriceSnapshotRepository snapshotRepository
	) {
		this.keywordRepository = keywordRepository;
		this.snapshotRepository = snapshotRepository;
	}

	@Transactional(readOnly = true)
	public KeywordAnalysisResponse getAnalysis(Long userId, Long keywordId) {
		Keyword keyword = keywordRepository.findByIdAndUserId(keywordId, userId)
				.filter(foundKeyword -> foundKeyword.getStatus() == KeywordStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
		ShoppingAnalysisResponse shopping = snapshotRepository.findFirstByKeyword_IdOrderByBaseDateDesc(keywordId)
				.map(this::toShoppingAnalysisResponse)
				.orElse(null);
		return new KeywordAnalysisResponse(
				keyword.getId(),
				keyword.getKeyword(),
				keyword.getAnalysisStatus(),
				keyword.getLastAnalyzedAt(),
				shopping,
				null,
				null
		);
	}

	private ShoppingAnalysisResponse toShoppingAnalysisResponse(ShoppingPriceSnapshot snapshot) {
		return new ShoppingAnalysisResponse(
				snapshot.getBaseDate(),
				snapshot.getTotalResults(),
				snapshot.getMinPrice(),
				snapshot.getMaxPrice(),
				snapshot.getAvgPrice(),
				snapshot.getTopItems().stream()
						.map(this::toTopItemResponse)
						.toList()
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
