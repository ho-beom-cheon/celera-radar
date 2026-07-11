package com.sellerradar.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.trend.client.NaverDataLabTimeUnit;
import com.sellerradar.trend.service.NaverShoppingCategoryCodeResolver;
import com.sellerradar.trend.service.TrendSnapshotService;
import com.sellerradar.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class DailyTrendBatchServiceTest {
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);

	private KeywordRepository keywordRepository;
	private TrendSnapshotService trendSnapshotService;
	private BatchJobHistoryRepository historyRepository;
	private DailyTrendBatchService batchService;

	@BeforeEach
	void setUp() {
		keywordRepository = mock(KeywordRepository.class);
		trendSnapshotService = mock(TrendSnapshotService.class);
		historyRepository = mock(BatchJobHistoryRepository.class);
		batchService = new DailyTrendBatchService(
				keywordRepository,
				trendSnapshotService,
				historyRepository,
				new NaverShoppingCategoryCodeResolver(),
				FIXED_CLOCK,
				2
		);
		when(historyRepository.save(any(BatchJobHistory.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void runManualDatalabTrendCollectsLimitedActiveKeywords() {
		Keyword defaultCategoryKeyword = keyword(1L, "desk tray", "CAR_ACCESSORY");
		Keyword naverCategoryKeyword = keyword(2L, "camping mat", "50000001");
		when(keywordRepository.findByActiveTrueAndDeletedAtIsNullOrderByLastAnalyzedAtAscCreatedAtAsc(any(Pageable.class)))
				.thenReturn(List.of(defaultCategoryKeyword, naverCategoryKeyword));

		BatchJobHistoryResponse response = batchService.runManualDatalabTrend();

		assertThat(response.jobType()).isEqualTo(BatchJobType.DATALAB_TREND_DAILY);
		assertThat(response.triggerType()).isEqualTo(BatchTriggerType.MANUAL);
		assertThat(response.status()).isEqualTo(BatchJobStatus.SUCCESS);
		assertThat(response.targetCount()).isEqualTo(2);
		assertThat(response.successCount()).isEqualTo(2);
		assertThat(response.failureCount()).isZero();

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(keywordRepository).findByActiveTrueAndDeletedAtIsNullOrderByLastAnalyzedAtAscCreatedAtAsc(
				pageableCaptor.capture()
		);
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
		verify(trendSnapshotService).collectKeywordTrend(
				1L,
				"50000008",
				LocalDate.of(2026, 6, 2),
				LocalDate.of(2026, 7, 2),
				NaverDataLabTimeUnit.DATE
		);
		verify(trendSnapshotService).collectKeywordTrend(
				2L,
				"50000001",
				LocalDate.of(2026, 6, 2),
				LocalDate.of(2026, 7, 2),
				NaverDataLabTimeUnit.DATE
		);
	}

	@Test
	void runManualDatalabTrendRecordsFailureCountAndContinues() {
		Keyword firstKeyword = keyword(1L, "desk tray", null);
		Keyword secondKeyword = keyword(2L, "camping mat", null);
		when(keywordRepository.findByActiveTrueAndDeletedAtIsNullOrderByLastAnalyzedAtAscCreatedAtAsc(any(Pageable.class)))
				.thenReturn(List.of(firstKeyword, secondKeyword));
		when(trendSnapshotService.collectKeywordTrend(
				2L,
				"50000008",
				LocalDate.of(2026, 6, 2),
				LocalDate.of(2026, 7, 2),
				NaverDataLabTimeUnit.DATE
		)).thenThrow(new IllegalStateException("api failed"));

		BatchJobHistoryResponse response = batchService.runManualDatalabTrend();

		assertThat(response.status()).isEqualTo(BatchJobStatus.PARTIAL_SUCCESS);
		assertThat(response.targetCount()).isEqualTo(2);
		assertThat(response.successCount()).isEqualTo(1);
		assertThat(response.failureCount()).isEqualTo(1);
		assertThat(response.errorMessage()).isEqualTo("failedKeywords=1");

		ArgumentCaptor<BatchJobHistory> historyCaptor = ArgumentCaptor.forClass(BatchJobHistory.class);
		verify(historyRepository, org.mockito.Mockito.times(2)).save(historyCaptor.capture());
		BatchJobHistory savedHistory = historyCaptor.getAllValues().getLast();
		assertThat(savedHistory.getFailureCount()).isEqualTo(1);
	}

	private Keyword keyword(Long id, String value, String category) {
		User user = User.create("seller" + id + "@example.com", "{bcrypt}hash");
		Keyword keyword = Keyword.create(user, value, value, category);
		ReflectionTestUtils.setField(keyword, "id", id);
		return keyword;
	}
}
