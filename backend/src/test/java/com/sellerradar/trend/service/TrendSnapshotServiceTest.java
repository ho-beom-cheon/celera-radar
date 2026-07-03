package com.sellerradar.trend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.domain.ApiCallLog;
import com.sellerradar.common.external.domain.ApiCallStatus;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.trend.client.NaverDataLabClient;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendRequest;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendResponse;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendResult;
import com.sellerradar.trend.client.NaverDataLabTimeUnit;
import com.sellerradar.trend.client.NaverDataLabTrendPoint;
import com.sellerradar.trend.domain.TrendSnapshot;
import com.sellerradar.trend.domain.TrendTimeUnit;
import com.sellerradar.trend.repository.TrendSnapshotRepository;
import com.sellerradar.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TrendSnapshotServiceTest {
	private static final long KEYWORD_ID = 10L;
	private static final String NAVER_CATEGORY_CODE = "50000000";
	private static final LocalDate START_DATE = LocalDate.of(2026, 6, 1);
	private static final LocalDate END_DATE = LocalDate.of(2026, 7, 1);

	private KeywordRepository keywordRepository;
	private TrendSnapshotRepository trendSnapshotRepository;
	private ApiCallLogRepository apiCallLogRepository;
	private NaverDataLabClient naverDataLabClient;
	private TrendSnapshotService service;
	private Keyword keyword;

	@BeforeEach
	void setUp() {
		keywordRepository = mock(KeywordRepository.class);
		trendSnapshotRepository = mock(TrendSnapshotRepository.class);
		apiCallLogRepository = mock(ApiCallLogRepository.class);
		naverDataLabClient = mock(NaverDataLabClient.class);
		service = new TrendSnapshotService(
				keywordRepository,
				trendSnapshotRepository,
				apiCallLogRepository,
				naverDataLabClient,
				new TrendScoreCalculator()
		);
		User user = User.create("seller@example.com", "{bcrypt}hash");
		keyword = Keyword.create(
				user,
				"차량용 수납함",
				"차량용 수납함",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		);
		ReflectionTestUtils.setField(keyword, "id", KEYWORD_ID);
		when(keywordRepository.findById(KEYWORD_ID)).thenReturn(Optional.of(keyword));
	}

	@Test
	void collectKeywordTrendStoresSnapshotsAndReturnsScore() {
		when(naverDataLabClient.searchKeywordTrend(any(NaverDataLabKeywordTrendRequest.class)))
				.thenReturn(keywordTrendResponse());
		when(trendSnapshotRepository.findByKeyword_IdAndPeriodAndTimeUnit(any(), any(), any()))
				.thenReturn(Optional.empty());
		when(trendSnapshotRepository.save(any(TrendSnapshot.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		TrendSnapshotCollectResult result = service.collectKeywordTrend(
				KEYWORD_ID,
				NAVER_CATEGORY_CODE,
				START_DATE,
				END_DATE,
				NaverDataLabTimeUnit.DATE
		);

		assertThat(result.keywordId()).isEqualTo(KEYWORD_ID);
		assertThat(result.savedCount()).isEqualTo(3);
		assertThat(result.points()).hasSize(3);
		assertThat(result.score().trendDelta7d()).isEqualTo(40.0);
		assertThat(result.score().trendDelta30d()).isEqualTo(70.0);
		assertThat(result.score().trendScore()).isEqualTo(17);
		assertThat(result.score().warnings()).containsExactly(TrendScoreCalculator.DATALAB_SALES_WARNING);

		ArgumentCaptor<NaverDataLabKeywordTrendRequest> requestCaptor =
				ArgumentCaptor.forClass(NaverDataLabKeywordTrendRequest.class);
		verify(naverDataLabClient).searchKeywordTrend(requestCaptor.capture());
		assertThat(requestCaptor.getValue().category()).isEqualTo(NAVER_CATEGORY_CODE);
		assertThat(requestCaptor.getValue().keyword()).isEqualTo("차량용 수납함");

		ArgumentCaptor<TrendSnapshot> snapshotCaptor = ArgumentCaptor.forClass(TrendSnapshot.class);
		verify(trendSnapshotRepository, org.mockito.Mockito.times(3)).save(snapshotCaptor.capture());
		assertThat(snapshotCaptor.getAllValues())
				.extracting(TrendSnapshot::getTimeUnit)
				.containsOnly(TrendTimeUnit.DATE);

		ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);
		verify(apiCallLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getProvider()).isEqualTo(ExternalApiProvider.NAVER_DATALAB);
		assertThat(logCaptor.getValue().getApiName()).isEqualTo(NaverDataLabClient.KEYWORD_TREND_API_NAME);
		assertThat(logCaptor.getValue().getStatus()).isEqualTo(ApiCallStatus.SUCCESS);
	}

	@Test
	void collectKeywordTrendUpdatesExistingSnapshotInsteadOfDuplicating() {
		TrendSnapshot existingSnapshot = TrendSnapshot.create(
				keyword,
				LocalDate.of(2026, 7, 1),
				TrendTimeUnit.DATE,
				new BigDecimal("10.0000")
		);
		when(naverDataLabClient.searchKeywordTrend(any(NaverDataLabKeywordTrendRequest.class)))
				.thenReturn(new NaverDataLabKeywordTrendResponse(
						"2026-07-01",
						"2026-07-01",
						"date",
						List.of(new NaverDataLabKeywordTrendResult(
								"차량용 수납함",
								List.of("차량용 수납함"),
								List.of(new NaverDataLabTrendPoint("2026-07-01", new BigDecimal("91.4000")))
						))
				));
		when(trendSnapshotRepository.findByKeyword_IdAndPeriodAndTimeUnit(
				KEYWORD_ID,
				LocalDate.of(2026, 7, 1),
				TrendTimeUnit.DATE
		)).thenReturn(Optional.of(existingSnapshot));
		when(trendSnapshotRepository.save(any(TrendSnapshot.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.collectKeywordTrend(KEYWORD_ID, NAVER_CATEGORY_CODE, END_DATE, END_DATE, NaverDataLabTimeUnit.DATE);

		assertThat(existingSnapshot.getRatio()).isEqualByComparingTo("91.4000");
		verify(trendSnapshotRepository).save(existingSnapshot);
	}

	@Test
	void collectKeywordTrendStoresFailureLogWhenClientFails() {
		when(naverDataLabClient.searchKeywordTrend(any(NaverDataLabKeywordTrendRequest.class)))
				.thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT));

		assertThatThrownBy(() -> service.collectKeywordTrend(
				KEYWORD_ID,
				NAVER_CATEGORY_CODE,
				START_DATE,
				END_DATE,
				NaverDataLabTimeUnit.DATE
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));

		ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);
		verify(apiCallLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getStatus()).isEqualTo(ApiCallStatus.FAILED);
		assertThat(logCaptor.getValue().getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT.name());
	}

	private NaverDataLabKeywordTrendResponse keywordTrendResponse() {
		return new NaverDataLabKeywordTrendResponse(
				"2026-06-01",
				"2026-07-01",
				"date",
				List.of(new NaverDataLabKeywordTrendResult(
						"차량용 수납함",
						List.of("차량용 수납함"),
						List.of(
								new NaverDataLabTrendPoint("2026-06-01", new BigDecimal("20.0000")),
								new NaverDataLabTrendPoint("2026-06-24", new BigDecimal("50.0000")),
								new NaverDataLabTrendPoint("2026-07-01", new BigDecimal("90.0000"))
						)
				))
		);
	}
}
