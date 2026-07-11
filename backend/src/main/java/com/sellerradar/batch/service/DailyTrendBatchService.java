package com.sellerradar.batch.service;

import com.sellerradar.batch.domain.BatchJobHistory;
import com.sellerradar.batch.domain.BatchJobType;
import com.sellerradar.batch.domain.BatchTriggerType;
import com.sellerradar.batch.dto.BatchJobHistoryResponse;
import com.sellerradar.batch.repository.BatchJobHistoryRepository;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.trend.client.NaverDataLabTimeUnit;
import com.sellerradar.trend.service.NaverShoppingCategoryCodeResolver;
import com.sellerradar.trend.service.TrendSnapshotService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DailyTrendBatchService {
	private static final int TREND_LOOKBACK_DAYS = 30;

	private final KeywordRepository keywordRepository;
	private final TrendSnapshotService trendSnapshotService;
	private final BatchJobHistoryRepository historyRepository;
	private final NaverShoppingCategoryCodeResolver categoryCodeResolver;
	private final Clock clock;
	private final int dailyTargetLimit;

	@Autowired
	public DailyTrendBatchService(
			KeywordRepository keywordRepository,
			TrendSnapshotService trendSnapshotService,
			BatchJobHistoryRepository historyRepository,
			NaverShoppingCategoryCodeResolver categoryCodeResolver,
			@Value("${seller-radar.batch.datalab-daily-target-limit:50}") int dailyTargetLimit
	) {
		this(
				keywordRepository,
				trendSnapshotService,
				historyRepository,
				categoryCodeResolver,
				Clock.systemDefaultZone(),
				dailyTargetLimit
		);
	}

	DailyTrendBatchService(
			KeywordRepository keywordRepository,
			TrendSnapshotService trendSnapshotService,
			BatchJobHistoryRepository historyRepository,
			NaverShoppingCategoryCodeResolver categoryCodeResolver,
			Clock clock,
			int dailyTargetLimit
	) {
		this.keywordRepository = keywordRepository;
		this.trendSnapshotService = trendSnapshotService;
		this.historyRepository = historyRepository;
		this.categoryCodeResolver = categoryCodeResolver;
		this.clock = clock;
		this.dailyTargetLimit = Math.max(dailyTargetLimit, 0);
	}

	public BatchJobHistoryResponse runManualDatalabTrend() {
		return run(BatchTriggerType.MANUAL);
	}

	public BatchJobHistoryResponse runScheduledDatalabTrend() {
		return run(BatchTriggerType.SCHEDULED);
	}

	private BatchJobHistoryResponse run(BatchTriggerType triggerType) {
		LocalDate endDate = LocalDate.now(clock);
		LocalDate startDate = endDate.minusDays(TREND_LOOKBACK_DAYS);
		List<Keyword> targetKeywords = targetKeywords();
		BatchJobHistory history = historyRepository.save(BatchJobHistory.start(
				BatchJobType.DATALAB_TREND_DAILY,
				triggerType,
				targetKeywords.size(),
				OffsetDateTime.now(clock)
		));

		int successCount = 0;
		int failureCount = 0;
		for (Keyword keyword : targetKeywords) {
			try {
				trendSnapshotService.collectKeywordTrend(
						keyword.getId(),
						resolveNaverCategoryCode(keyword),
						startDate,
						endDate,
						NaverDataLabTimeUnit.DATE
				);
				successCount++;
			} catch (RuntimeException exception) {
				failureCount++;
			}
		}

		history.complete(successCount, failureCount, OffsetDateTime.now(clock));
		return BatchJobHistoryResponse.from(historyRepository.save(history));
	}

	private List<Keyword> targetKeywords() {
		if (dailyTargetLimit <= 0) {
			return List.of();
		}
		return keywordRepository.findByActiveTrueAndDeletedAtIsNullOrderByLastAnalyzedAtAscCreatedAtAsc(
				PageRequest.of(0, dailyTargetLimit)
		);
	}

	private String resolveNaverCategoryCode(Keyword keyword) {
		return categoryCodeResolver.resolve(keyword.getCategory());
	}
}
