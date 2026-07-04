package com.sellerradar.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.batch.domain.BatchJobHistory;
import com.sellerradar.batch.domain.BatchJobStatus;
import com.sellerradar.batch.domain.BatchJobType;
import com.sellerradar.batch.domain.BatchTriggerType;
import com.sellerradar.batch.dto.BatchJobHistoryResponse;
import com.sellerradar.batch.repository.BatchJobHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlertGenerateBatchServiceTest {
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);

	private AlertGenerationService alertGenerationService;
	private BatchJobHistoryRepository historyRepository;
	private AlertGenerateBatchService batchService;

	@BeforeEach
	void setUp() {
		alertGenerationService = mock(AlertGenerationService.class);
		historyRepository = mock(BatchJobHistoryRepository.class);
		batchService = new AlertGenerateBatchService(alertGenerationService, historyRepository, FIXED_CLOCK);
		when(historyRepository.save(any(BatchJobHistory.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void runScheduledStoresRuleCountAndGeneratedCount() {
		when(alertGenerationService.generateDaily())
				.thenReturn(new AlertGenerationResult(3, 7));

		BatchJobHistoryResponse response = batchService.runScheduled();

		assertThat(response.jobType()).isEqualTo(BatchJobType.ALERT_GENERATE_DAILY);
		assertThat(response.triggerType()).isEqualTo(BatchTriggerType.SCHEDULED);
		assertThat(response.status()).isEqualTo(BatchJobStatus.SUCCESS);
		assertThat(response.targetCount()).isEqualTo(3);
		assertThat(response.successCount()).isEqualTo(7);
		assertThat(response.failureCount()).isZero();
		assertThat(response.errorMessage()).isNull();
	}

	@Test
	void runScheduledRecordsFailedHistoryAndRethrows() {
		when(alertGenerationService.generateDaily())
				.thenThrow(new IllegalStateException("alert generation failed"));

		assertThatThrownBy(() -> batchService.runScheduled())
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("alert generation failed");

		ArgumentCaptor<BatchJobHistory> historyCaptor = ArgumentCaptor.forClass(BatchJobHistory.class);
		verify(historyRepository, org.mockito.Mockito.times(2)).save(historyCaptor.capture());
		BatchJobHistory failedHistory = historyCaptor.getAllValues().getLast();
		assertThat(failedHistory.getJobType()).isEqualTo(BatchJobType.ALERT_GENERATE_DAILY);
		assertThat(failedHistory.getStatus()).isEqualTo(BatchJobStatus.FAILED);
		assertThat(failedHistory.getFailureCount()).isEqualTo(1);
		assertThat(failedHistory.getErrorMessage()).isEqualTo("alert generation failed");
		assertThat(failedHistory.getFinishedAt()).isNotNull();
	}
}
