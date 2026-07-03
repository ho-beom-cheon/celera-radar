package com.sellerradar.alert.service;

import com.sellerradar.batch.domain.BatchJobHistory;
import com.sellerradar.batch.domain.BatchJobType;
import com.sellerradar.batch.domain.BatchTriggerType;
import com.sellerradar.batch.dto.BatchJobHistoryResponse;
import com.sellerradar.batch.repository.BatchJobHistoryRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlertGenerateBatchService {
	private final AlertGenerationService alertGenerationService;
	private final BatchJobHistoryRepository historyRepository;
	private final Clock clock;

	@Autowired
	public AlertGenerateBatchService(
			AlertGenerationService alertGenerationService,
			BatchJobHistoryRepository historyRepository
	) {
		this(alertGenerationService, historyRepository, Clock.systemDefaultZone());
	}

	AlertGenerateBatchService(
			AlertGenerationService alertGenerationService,
			BatchJobHistoryRepository historyRepository,
			Clock clock
	) {
		this.alertGenerationService = alertGenerationService;
		this.historyRepository = historyRepository;
		this.clock = clock;
	}

	public BatchJobHistoryResponse runScheduled() {
		BatchJobHistory history = historyRepository.save(BatchJobHistory.start(
				BatchJobType.ALERT_GENERATE_DAILY,
				BatchTriggerType.SCHEDULED,
				0,
				OffsetDateTime.now(clock)
		));
		AlertGenerationResult result = alertGenerationService.generateDaily();
		history.complete(result.generatedCount(), 0, OffsetDateTime.now(clock));
		return BatchJobHistoryResponse.from(historyRepository.save(history));
	}
}
