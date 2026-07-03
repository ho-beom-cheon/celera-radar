package com.sellerradar.keyword.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.dto.KeywordCreateRequest;
import com.sellerradar.keyword.dto.KeywordResponse;
import com.sellerradar.keyword.dto.KeywordUpdateRequest;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.shopping.repository.ShoppingPriceSnapshotRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class KeywordServiceTest {
	private static final long USER_ID = 1L;

	private KeywordRepository keywordRepository;
	private UserRepository userRepository;
	private ShoppingPriceSnapshotRepository snapshotRepository;
	private KeywordService keywordService;
	private User user;

	@BeforeEach
	void setUp() {
		keywordRepository = mock(KeywordRepository.class);
		userRepository = mock(UserRepository.class);
		snapshotRepository = mock(ShoppingPriceSnapshotRepository.class);
		keywordService = new KeywordService(keywordRepository, userRepository, new KeywordNormalizer(), snapshotRepository);
		user = User.create("seller@example.com", "{bcrypt}hash");
		ReflectionTestUtils.setField(user, "id", USER_ID);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(snapshotRepository.findFirstByKeyword_IdOrderBySearchDateDesc(any())).thenReturn(Optional.empty());
	}

	@Test
	void createKeywordUsesStringCategoryAndPendingStatus() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(
				USER_ID,
				"car storage box"
		)).thenReturn(false);
		when(keywordRepository.countByUserIdAndActiveTrueAndDeletedAtIsNull(USER_ID)).thenReturn(0L);
		when(keywordRepository.save(any(Keyword.class))).thenAnswer(invocation -> invocation.getArgument(0));

		KeywordResponse response = keywordService.create(
				USER_ID,
				new KeywordCreateRequest("  car   storage box  ", " Car Gear ")
		);

		assertThat(response.keyword()).isEqualTo("car storage box");
		assertThat(response.category()).isEqualTo("Car Gear");
		assertThat(response.active()).isTrue();
		assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.PENDING);
	}

	@Test
	void createKeywordStoresBlankCategoryAsNull() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(
				USER_ID,
				"car storage box"
		)).thenReturn(false);
		when(keywordRepository.countByUserIdAndActiveTrueAndDeletedAtIsNull(USER_ID)).thenReturn(0L);
		when(keywordRepository.save(any(Keyword.class))).thenAnswer(invocation -> invocation.getArgument(0));

		KeywordResponse response = keywordService.create(
				USER_ID,
				new KeywordCreateRequest("car storage box", "   ")
		);

		assertThat(response.category()).isNull();
	}

	@Test
	void createKeywordRejectsDuplicatedActiveKeyword() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(
				USER_ID,
				"car storage box"
		)).thenReturn(true);

		assertThatThrownBy(() -> keywordService.create(
				USER_ID,
				new KeywordCreateRequest("car storage box", "Car Gear")
		))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATED_KEYWORD));

		verify(keywordRepository, never()).save(any(Keyword.class));
	}

	@Test
	void createKeywordRejectsFreePlanLimitExceeded() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(
				USER_ID,
				"car storage box"
		)).thenReturn(false);
		when(keywordRepository.countByUserIdAndActiveTrueAndDeletedAtIsNull(USER_ID)).thenReturn(3L);

		assertThatThrownBy(() -> keywordService.create(
				USER_ID,
				new KeywordCreateRequest("car storage box", "Car Gear")
		))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.KEYWORD_LIMIT_EXCEEDED));
	}

	@Test
	void updateKeywordKeepsSameNormalizedKeywordWithoutDuplicateCheck() {
		Keyword keyword = Keyword.create(user, "car storage box", "car storage box", "Car Gear");
		ReflectionTestUtils.setField(keyword, "id", 10L);
		when(keywordRepository.findByIdAndUserIdAndActiveTrueAndDeletedAtIsNull(10L, USER_ID))
				.thenReturn(Optional.of(keyword));

		KeywordResponse response = keywordService.update(
				USER_ID,
				10L,
				new KeywordUpdateRequest(" car  storage box ", " Storage ")
		);

		assertThat(response.keyword()).isEqualTo("car storage box");
		assertThat(response.category()).isEqualTo("Storage");
		verify(keywordRepository, never())
				.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(any(), any());
	}

	@Test
	void deleteKeywordSoftDeletesKeyword() {
		Keyword keyword = Keyword.create(user, "car storage box", "car storage box", "Car Gear");
		when(keywordRepository.findByIdAndUserIdAndActiveTrueAndDeletedAtIsNull(10L, USER_ID))
				.thenReturn(Optional.of(keyword));

		keywordService.delete(USER_ID, 10L);

		assertThat(keyword.isActive()).isFalse();
		assertThat(keyword.getDeletedAt()).isNotNull();
	}

	@Test
	void listFiltersByCategoryAndAnalysisStatus() {
		PageRequest pageable = PageRequest.of(0, 20);
		when(keywordRepository.findByUserIdAndActiveTrueAndCategoryAndAnalysisStatusAndDeletedAtIsNull(
				USER_ID,
				"Car Gear",
				AnalysisStatus.PENDING,
				pageable
		)).thenReturn(Page.empty(pageable));

		keywordService.list(USER_ID, " Car Gear ", AnalysisStatus.PENDING, pageable);

		verify(keywordRepository).findByUserIdAndActiveTrueAndCategoryAndAnalysisStatusAndDeletedAtIsNull(
				USER_ID,
				"Car Gear",
				AnalysisStatus.PENDING,
				pageable
		);
	}
}
