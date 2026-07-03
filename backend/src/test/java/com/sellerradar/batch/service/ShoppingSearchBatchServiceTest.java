package com.sellerradar.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.batch.domain.BatchJobHistory;
import com.sellerradar.batch.domain.BatchJobStatus;
import com.sellerradar.batch.domain.BatchJobType;
import com.sellerradar.batch.domain.BatchTriggerType;
import com.sellerradar.batch.dto.BatchJobHistoryResponse;
import com.sellerradar.batch.repository.BatchJobHistoryRepository;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.service.ShoppingSearchSnapshotService;
import com.sellerradar.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ShoppingSearchBatchServiceTest {
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);

	private KeywordRepository keywordRepository;
	private ShoppingSearchSnapshotService snapshotService;
	private BatchJobHistoryRepository historyRepository;
	private ShoppingSearchBatchService batchService;

	@BeforeEach
	void setUp() {
		keywordRepository = mock(KeywordRepository.class);
		snapshotService = mock(ShoppingSearchSnapshotService.class);
		historyRepository = mock(BatchJobHistoryRepository.class);
		batchService = new ShoppingSearchBatchService(
				keywordRepository,
				snapshotService,
				historyRepository,
				FIXED_CLOCK
		);
		when(historyRepository.save(org.mockito.ArgumentMatchers.any(BatchJobHistory.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void runManualShoppingSearchCollectsOnlyActiveKeywords() {
		Keyword firstKeyword = keyword(1L, "차량용 수납함");
		Keyword secondKeyword = keyword(2L, "케이블 정리함");
		when(keywordRepository.findByStatus(KeywordStatus.ACTIVE)).thenReturn(List.of(firstKeyword, secondKeyword));

		BatchJobHistoryResponse response = batchService.runManualShoppingSearch();

		assertThat(response.jobType()).isEqualTo(BatchJobType.SHOPPING_SEARCH_DAILY);
		assertThat(response.triggerType()).isEqualTo(BatchTriggerType.MANUAL);
		assertThat(response.status()).isEqualTo(BatchJobStatus.SUCCESS);
		assertThat(response.targetCount()).isEqualTo(2);
		assertThat(response.successCount()).isEqualTo(2);
		assertThat(response.failureCount()).isZero();
		verify(keywordRepository).findByStatus(KeywordStatus.ACTIVE);
		verify(snapshotService).collect(1L, LocalDate.of(2026, 7, 2));
		verify(snapshotService).collect(2L, LocalDate.of(2026, 7, 2));
	}

	@Test
	void runManualShoppingSearchRecordsFailureCountAndContinues() {
		Keyword firstKeyword = keyword(1L, "차량용 수납함");
		Keyword secondKeyword = keyword(2L, "케이블 정리함");
		when(keywordRepository.findByStatus(KeywordStatus.ACTIVE)).thenReturn(List.of(firstKeyword, secondKeyword));
		when(snapshotService.collect(2L, LocalDate.of(2026, 7, 2))).thenThrow(new IllegalStateException("api failed"));

		BatchJobHistoryResponse response = batchService.runManualShoppingSearch();

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

	private Keyword keyword(Long id, String value) {
		User user = User.create("seller" + id + "@example.com", "{bcrypt}hash");
		Keyword keyword = Keyword.create(
				user,
				value,
				value,
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		);
		ReflectionTestUtils.setField(keyword, "id", id);
		return keyword;
	}
}
