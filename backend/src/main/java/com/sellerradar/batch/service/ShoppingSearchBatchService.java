package com.sellerradar.batch.service;

import com.sellerradar.batch.domain.BatchJobHistory;
import com.sellerradar.batch.domain.BatchJobType;
import com.sellerradar.batch.domain.BatchTriggerType;
import com.sellerradar.batch.dto.BatchJobHistoryResponse;
import com.sellerradar.batch.repository.BatchJobHistoryRepository;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.service.ShoppingSearchSnapshotService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ShoppingSearchBatchService {
	private final KeywordRepository keywordRepository;
	private final ShoppingSearchSnapshotService snapshotService;
	private final BatchJobHistoryRepository historyRepository;
	private final Clock clock;

	@Autowired
	public ShoppingSearchBatchService(
			KeywordRepository keywordRepository,
			ShoppingSearchSnapshotService snapshotService,
			BatchJobHistoryRepository historyRepository
	) {
		this(keywordRepository, snapshotService, historyRepository, Clock.systemDefaultZone());
	}

	ShoppingSearchBatchService(
			KeywordRepository keywordRepository,
			ShoppingSearchSnapshotService snapshotService,
			BatchJobHistoryRepository historyRepository,
			Clock clock
	) {
		this.keywordRepository = keywordRepository;
		this.snapshotService = snapshotService;
		this.historyRepository = historyRepository;
		this.clock = clock;
	}

	public BatchJobHistoryResponse runManualShoppingSearch() {
		LocalDate baseDate = LocalDate.now(clock);
		List<Keyword> activeKeywords = keywordRepository.findByStatus(KeywordStatus.ACTIVE);
		BatchJobHistory history = historyRepository.save(BatchJobHistory.start(
				BatchJobType.SHOPPING_SEARCH_DAILY,
				BatchTriggerType.MANUAL,
				activeKeywords.size(),
				OffsetDateTime.now(clock)
		));

		int successCount = 0;
		int failureCount = 0;
		for (Keyword keyword : activeKeywords) {
			try {
				snapshotService.collect(keyword.getId(), baseDate);
				successCount++;
			} catch (RuntimeException exception) {
				failureCount++;
			}
		}

		history.complete(successCount, failureCount, OffsetDateTime.now(clock));
		return BatchJobHistoryResponse.from(historyRepository.save(history));
	}

	public Page<BatchJobHistoryResponse> list(Pageable pageable) {
		return historyRepository.findAllByOrderByStartedAtDesc(pageable)
				.map(BatchJobHistoryResponse::from);
	}
}
