package com.sellerradar.shopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.domain.ApiCallLog;
import com.sellerradar.common.external.domain.ApiCallStatus;
import com.sellerradar.common.external.domain.ExternalApiProvider;
import com.sellerradar.common.external.repository.ApiCallLogRepository;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.client.NaverShoppingClient;
import com.sellerradar.shopping.client.NaverShoppingSearchItem;
import com.sellerradar.shopping.client.NaverShoppingSearchRequest;
import com.sellerradar.shopping.client.NaverShoppingSearchResponse;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class ShoppingSearchSnapshotServiceTest {
	private static final long KEYWORD_ID = 10L;
	private static final LocalDate BASE_DATE = LocalDate.of(2026, 7, 2);
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);

	private KeywordRepository keywordRepository;
	private ShoppingPriceSnapshotRepository snapshotRepository;
	private ApiCallLogRepository apiCallLogRepository;
	private NaverShoppingClient naverShoppingClient;
	private ShoppingSearchSnapshotService service;
	private Keyword keyword;

	@BeforeEach
	void setUp() {
		keywordRepository = mock(KeywordRepository.class);
		snapshotRepository = mock(ShoppingPriceSnapshotRepository.class);
		apiCallLogRepository = mock(ApiCallLogRepository.class);
		naverShoppingClient = mock(NaverShoppingClient.class);
		service = new ShoppingSearchSnapshotService(
				keywordRepository,
				snapshotRepository,
				apiCallLogRepository,
				naverShoppingClient,
				new ObjectMapper(),
				FIXED_CLOCK
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
	}

	@Test
	void collectUsesCachedSnapshotWithoutCallingExternalApi() {
		ShoppingPriceSnapshot cachedSnapshot = ShoppingPriceSnapshot.create(
				keyword,
				BASE_DATE,
				100L,
				1000,
				2000,
				1500,
				"{}"
		);
		when(snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(KEYWORD_ID, BASE_DATE, "sim"))
				.thenReturn(Optional.of(cachedSnapshot));

		ShoppingPriceSnapshot result = service.collect(KEYWORD_ID, BASE_DATE);

		assertThat(result).isSameAs(cachedSnapshot);
		verifyNoInteractions(naverShoppingClient);
		verify(apiCallLogRepository, never()).save(any());
		verify(keywordRepository, never()).save(any());
	}

	@Test
	void collectStoresSnapshotTopItemsAndApiCallLog() {
		when(snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(KEYWORD_ID, BASE_DATE, "sim"))
				.thenReturn(Optional.empty());
		when(keywordRepository.findById(KEYWORD_ID)).thenReturn(Optional.of(keyword));
		when(naverShoppingClient.search(any(NaverShoppingSearchRequest.class))).thenReturn(shoppingResponse());
		when(snapshotRepository.saveAndFlush(any(ShoppingPriceSnapshot.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ShoppingPriceSnapshot result = service.collect(KEYWORD_ID, BASE_DATE);

		assertThat(result.getTotalResults()).isEqualTo(1234);
		assertThat(result.getMinPrice()).isEqualTo(1000);
		assertThat(result.getMaxPrice()).isEqualTo(3000);
		assertThat(result.getAvgPrice()).isEqualTo(2000);
		assertThat(result.getMedianPrice()).isEqualTo(2000);
		assertThat(result.getSearchDate()).isEqualTo(BASE_DATE);
		assertThat(result.getSortType()).isEqualTo("sim");
		assertThat(result.getDisplayCount()).isEqualTo(100);
		assertThat(result.getCompetitionLevel()).hasToString("LOW");
		assertThat(result.getTopItems()).hasSize(2);
		assertThat(result.getTopItems().getFirst().getItemRank()).isEqualTo(1);
		assertThat(result.getTopItems().getFirst().getMallName()).isEqualTo("테스트몰");
		assertThat(keyword.getAnalysisStatus()).isEqualTo(AnalysisStatus.SUCCESS);
		assertThat(keyword.getLastAnalyzedAt()).isNotNull();

		ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);
		verify(apiCallLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getProvider()).isEqualTo(ExternalApiProvider.NAVER_SEARCH);
		assertThat(logCaptor.getValue().getApiName()).isEqualTo("NAVER_SHOPPING_SEARCH");
		assertThat(logCaptor.getValue().getStatus()).isEqualTo(ApiCallStatus.SUCCESS);
		assertThat(logCaptor.getValue().getHttpStatus()).isEqualTo(200);
		verify(keywordRepository).save(keyword);
	}

	@Test
	void collectStoresFailedApiCallLogWhenExternalApiFails() {
		when(snapshotRepository.findByKeyword_IdAndSearchDateAndSortType(KEYWORD_ID, BASE_DATE, "sim"))
				.thenReturn(Optional.empty());
		when(keywordRepository.findById(KEYWORD_ID)).thenReturn(Optional.of(keyword));
		when(naverShoppingClient.search(any(NaverShoppingSearchRequest.class)))
				.thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_RATE_LIMIT));

		assertThatThrownBy(() -> service.collect(KEYWORD_ID, BASE_DATE))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT));

		ArgumentCaptor<ApiCallLog> logCaptor = ArgumentCaptor.forClass(ApiCallLog.class);
		verify(apiCallLogRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getStatus()).isEqualTo(ApiCallStatus.FAILED);
		assertThat(logCaptor.getValue().getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_RATE_LIMIT.name());
		assertThat(keyword.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
		verify(keywordRepository).save(keyword);
	}

	private NaverShoppingSearchResponse shoppingResponse() {
		return new NaverShoppingSearchResponse(
				"Thu, 02 Jul 2026 20:00:00 +0900",
				1234L,
				1,
				2,
				List.of(
						new NaverShoppingSearchItem(
								"차량용 수납함 A",
								"https://example.com/a",
								"https://example.com/a.jpg",
								"1000",
								"1500",
								"테스트몰",
								"1000001",
								"1",
								"브랜드A",
								"제조사A",
								"자동차용품",
								"수납용품",
								"",
								""
						),
						new NaverShoppingSearchItem(
								"차량용 수납함 B",
								"https://example.com/b",
								"https://example.com/b.jpg",
								"3000",
								"3500",
								"샘플몰",
								"1000002",
								"1",
								"브랜드B",
								"제조사B",
								"자동차용품",
								"수납용품",
								"",
								""
						)
				)
		);
	}
}
