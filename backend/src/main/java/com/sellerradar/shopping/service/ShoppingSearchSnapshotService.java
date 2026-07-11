package com.sellerradar.shopping.service;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.domain.ApiCallLog;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.client.NaverShoppingSearchItem;
import com.sellerradar.shopping.client.NaverShoppingSearchRequest;
import com.sellerradar.shopping.client.NaverShoppingSearchResponse;
import com.sellerradar.shopping.client.NaverShoppingSort;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.domain.ShoppingTopItem;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.shopping.port.ShoppingSearchProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ShoppingSearchSnapshotService {
	private static final String API_NAME = "NAVER_SHOPPING_SEARCH";
	private static final int DEFAULT_DISPLAY = 100;
	private static final int DEFAULT_START = 1;
	private static final String DEFAULT_EXCLUDE = "used:rental:cbshop";

	private final KeywordRepository keywordRepository;
	private final ShoppingPriceSnapshotRepository snapshotRepository;
	private final ApiCallLogRepository apiCallLogRepository;
	private final ShoppingSearchProvider shoppingSearchProvider;
	private final CompetitionAnalyzer competitionAnalyzer;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	public ShoppingSearchSnapshotService(
			KeywordRepository keywordRepository,
			ShoppingPriceSnapshotRepository snapshotRepository,
			ApiCallLogRepository apiCallLogRepository,
			ShoppingSearchProvider shoppingSearchProvider,
			CompetitionAnalyzer competitionAnalyzer,
			ObjectMapper objectMapper
	) {
		this(
				keywordRepository,
				snapshotRepository,
				apiCallLogRepository,
				shoppingSearchProvider,
				competitionAnalyzer,
				objectMapper,
				Clock.systemDefaultZone()
		);
	}

	ShoppingSearchSnapshotService(
			KeywordRepository keywordRepository,
			ShoppingPriceSnapshotRepository snapshotRepository,
			ApiCallLogRepository apiCallLogRepository,
			ShoppingSearchProvider shoppingSearchProvider,
			CompetitionAnalyzer competitionAnalyzer,
			ObjectMapper objectMapper,
			Clock clock
	) {
		this.keywordRepository = keywordRepository;
		this.snapshotRepository = snapshotRepository;
		this.apiCallLogRepository = apiCallLogRepository;
		this.shoppingSearchProvider = shoppingSearchProvider;
		this.competitionAnalyzer = competitionAnalyzer;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	public ShoppingPriceSnapshot collect(Long keywordId) {
		return collect(keywordId, LocalDate.now(clock));
	}

	public ShoppingPriceSnapshot collect(Long keywordId, LocalDate baseDate) {
		return collectWithCacheStatus(keywordId, baseDate).snapshot();
	}

	public ShoppingSnapshotCollectResult collectWithCacheStatus(Long keywordId, LocalDate baseDate) {
		return snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(
						keywordId,
						baseDate,
						NaverShoppingSort.SIM.value()
				)
				.map(snapshot -> {
					markKeywordAnalyzed(keywordId, snapshot.getSearchDate(), OffsetDateTime.now(clock));
					return new ShoppingSnapshotCollectResult(snapshot, true);
				})
				.orElseGet(() -> new ShoppingSnapshotCollectResult(collectFreshSnapshot(keywordId, baseDate), false));
	}

	private ShoppingPriceSnapshot collectFreshSnapshot(Long keywordId, LocalDate baseDate) {
		Keyword keyword = keywordRepository.findById(keywordId)
				.filter(foundKeyword -> foundKeyword.getStatus() == KeywordStatus.ACTIVE)
				.orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));

		NaverShoppingSearchResponse response = searchNaver(keyword, baseDate);
		ShoppingPriceSnapshot snapshot = buildSnapshot(keyword, baseDate, response);
		try {
			ShoppingPriceSnapshot savedSnapshot = snapshotRepository.saveAndFlush(snapshot);
			markKeywordAnalyzed(keyword, savedSnapshot.getSearchDate(), OffsetDateTime.now(clock));
			return savedSnapshot;
		} catch (DataIntegrityViolationException exception) {
			ShoppingPriceSnapshot cachedSnapshot = snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(
							keywordId,
							baseDate,
							NaverShoppingSort.SIM.value()
					)
					.orElseThrow(() -> exception);
			markKeywordAnalyzed(keyword, cachedSnapshot.getSearchDate(), OffsetDateTime.now(clock));
			return cachedSnapshot;
		}
	}

	private NaverShoppingSearchResponse searchNaver(Keyword keyword, LocalDate baseDate) {
		try {
			NaverShoppingSearchResponse response = shoppingSearchProvider.search(new NaverShoppingSearchRequest(
					keyword.getKeyword(),
					DEFAULT_DISPLAY,
					DEFAULT_START,
					NaverShoppingSort.SIM,
					DEFAULT_EXCLUDE
			));
			apiCallLogRepository.save(ApiCallLog.success(
					ExternalApiProvider.NAVER_SEARCH,
					API_NAME,
					keyword.getId(),
					baseDate
			));
			return response;
		} catch (RuntimeException exception) {
			recordApiFailure(keyword, baseDate, exception);
			keyword.markAnalysisFailed(OffsetDateTime.now(clock));
			keywordRepository.save(keyword);
			throw exception;
		}
	}

	private void recordApiFailure(Keyword keyword, LocalDate baseDate, RuntimeException exception) {
		ErrorCode errorCode = exception instanceof BusinessException businessException
				? businessException.errorCode()
				: ErrorCode.EXTERNAL_API_UNAVAILABLE;
		apiCallLogRepository.save(ApiCallLog.failure(
				ExternalApiProvider.NAVER_SEARCH,
				API_NAME,
				keyword.getId(),
				baseDate,
				errorCode.status().value(),
				errorCode.name(),
				exception.getMessage()
		));
	}

	private void markKeywordAnalyzed(Long keywordId, LocalDate snapshotDate, OffsetDateTime analyzedAt) {
		keywordRepository.findById(keywordId)
				.filter(foundKeyword -> foundKeyword.getStatus() == KeywordStatus.ACTIVE)
				.ifPresent(keyword -> markKeywordAnalyzed(keyword, snapshotDate, analyzedAt));
	}

	private void markKeywordAnalyzed(Keyword keyword, LocalDate snapshotDate, OffsetDateTime analyzedAt) {
		keyword.markAnalyzed(analyzedAt);
		keyword.updateLastSnapshotDate(snapshotDate);
		keywordRepository.save(keyword);
	}

	private ShoppingPriceSnapshot buildSnapshot(
			Keyword keyword,
			LocalDate baseDate,
			NaverShoppingSearchResponse response
	) {
		List<NaverShoppingSearchItem> items = response.items() == null ? List.of() : response.items();
		List<Integer> prices = items.stream()
				.map(NaverShoppingSearchItem::lprice)
				.map(this::parsePrice)
				.filter(price -> price != null && price > 0)
				.toList();
		ShoppingPriceSnapshot snapshot = ShoppingPriceSnapshot.createSuccess(
				keyword,
				baseDate,
				keyword.getKeyword(),
				NaverShoppingSort.SIM.value(),
				DEFAULT_DISPLAY,
				toInteger(response.total()),
				min(prices),
				max(prices),
				average(prices),
				median(prices),
				rawJson(response),
				OffsetDateTime.now(clock),
				false
		);
		for (int index = 0; index < items.size(); index++) {
			snapshot.addTopItem(toTopItem(index + 1, items.get(index)));
		}
		snapshot.updateCompetitionLevel(
				competitionAnalyzer.analyze(snapshot.getTotalCount(), snapshot.getTopItems().size())
		);
		return snapshot;
	}

	private ShoppingTopItem toTopItem(int itemRank, NaverShoppingSearchItem item) {
		return ShoppingTopItem.create(
				itemRank,
				item.title(),
				item.link(),
				item.image(),
				parsePrice(item.lprice()),
				parsePrice(item.hprice()),
				item.mallName(),
				item.productId(),
				item.productType(),
				item.brand(),
				item.maker(),
				item.category1(),
				item.category2(),
				item.category3(),
				item.category4()
		);
	}

	private Integer parsePrice(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private Integer min(List<Integer> prices) {
		return prices.stream().min(Integer::compareTo).orElse(null);
	}

	private Integer max(List<Integer> prices) {
		return prices.stream().max(Integer::compareTo).orElse(null);
	}

	private Integer average(List<Integer> prices) {
		if (prices.isEmpty()) {
			return null;
		}
		return (int) Math.round(prices.stream()
				.mapToInt(Integer::intValue)
				.average()
				.orElse(0));
	}

	private Integer median(List<Integer> prices) {
		if (prices.isEmpty()) {
			return null;
		}
		List<Integer> sortedPrices = prices.stream().sorted().toList();
		int middle = sortedPrices.size() / 2;
		if (sortedPrices.size() % 2 == 1) {
			return sortedPrices.get(middle);
		}
		return (sortedPrices.get(middle - 1) + sortedPrices.get(middle)) / 2;
	}

	private Integer toInteger(long value) {
		if (value > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) value;
	}

	private String rawJson(NaverShoppingSearchResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JacksonException exception) {
			throw new IllegalStateException("네이버 쇼핑 검색 응답 직렬화에 실패했습니다.", exception);
		}
	}
}
