package com.sellerradar.keyword.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import com.sellerradar.keyword.domain.KeywordStatus;
import com.sellerradar.keyword.dto.KeywordCreateRequest;
import com.sellerradar.keyword.dto.KeywordResponse;
import com.sellerradar.keyword.dto.KeywordUpdateRequest;
import com.sellerradar.keyword.repository.KeywordRepository;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class KeywordServiceTest {
	private static final long USER_ID = 1L;

	private KeywordRepository keywordRepository;
	private UserRepository userRepository;
	private KeywordService keywordService;
	private User user;

	@BeforeEach
	void setUp() {
		keywordRepository = mock(KeywordRepository.class);
		userRepository = mock(UserRepository.class);
		keywordService = new KeywordService(keywordRepository, userRepository, new KeywordNormalizer());
		user = User.create("seller@example.com", "{bcrypt}hash");
		ReflectionTestUtils.setField(user, "id", USER_ID);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
	}

	@Test
	void createKeywordUsesFreePlanDefaults() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndStatus(USER_ID, "차량용 수납함", KeywordStatus.ACTIVE))
				.thenReturn(false);
		when(keywordRepository.countByUserIdAndStatus(USER_ID, KeywordStatus.ACTIVE)).thenReturn(0L);
		when(keywordRepository.save(any(Keyword.class))).thenAnswer(invocation -> invocation.getArgument(0));

		KeywordResponse response = keywordService.create(
				USER_ID,
				new KeywordCreateRequest("  차량용   수납함  ", CategoryCode.CAR_ACCESSORY, null)
		);

		assertThat(response.keyword()).isEqualTo("차량용 수납함");
		assertThat(response.categoryCode()).isEqualTo(CategoryCode.CAR_ACCESSORY);
		assertThat(response.priority()).isEqualTo(KeywordPriority.MEDIUM);
		assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.PENDING);
	}

	@Test
	void createKeywordRejectsDuplicatedActiveKeyword() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndStatus(USER_ID, "차량용 수납함", KeywordStatus.ACTIVE))
				.thenReturn(true);

		assertThatThrownBy(() -> keywordService.create(
				USER_ID,
				new KeywordCreateRequest("차량용 수납함", CategoryCode.CAR_ACCESSORY, KeywordPriority.HIGH)
		))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATED_KEYWORD));

		verify(keywordRepository, never()).save(any(Keyword.class));
	}

	@Test
	void createKeywordRejectsFreePlanLimitExceeded() {
		when(keywordRepository.existsByUserIdAndNormalizedKeywordAndStatus(USER_ID, "차량용 수납함", KeywordStatus.ACTIVE))
				.thenReturn(false);
		when(keywordRepository.countByUserIdAndStatus(USER_ID, KeywordStatus.ACTIVE)).thenReturn(3L);

		assertThatThrownBy(() -> keywordService.create(
				USER_ID,
				new KeywordCreateRequest("차량용 수납함", CategoryCode.CAR_ACCESSORY, KeywordPriority.HIGH)
		))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.KEYWORD_LIMIT_EXCEEDED));
	}

	@Test
	void updateKeywordKeepsSameNormalizedKeywordWithoutDuplicateCheck() {
		Keyword keyword = Keyword.create(
				user,
				"차량용 수납함",
				"차량용 수납함",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		);
		ReflectionTestUtils.setField(keyword, "id", 10L);
		when(keywordRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(keyword));

		KeywordResponse response = keywordService.update(
				USER_ID,
				10L,
				new KeywordUpdateRequest(" 차량용  수납함 ", CategoryCode.HOME_STORAGE, KeywordPriority.HIGH)
		);

		assertThat(response.keyword()).isEqualTo("차량용 수납함");
		assertThat(response.categoryCode()).isEqualTo(CategoryCode.HOME_STORAGE);
		assertThat(response.priority()).isEqualTo(KeywordPriority.HIGH);
		verify(keywordRepository, never())
				.existsByUserIdAndNormalizedKeywordAndStatus(any(), any(), any());
	}

	@Test
	void deleteKeywordSoftDeletesKeyword() {
		Keyword keyword = Keyword.create(
				user,
				"차량용 수납함",
				"차량용 수납함",
				CategoryCode.CAR_ACCESSORY,
				KeywordPriority.MEDIUM
		);
		when(keywordRepository.findByIdAndUserId(10L, USER_ID)).thenReturn(Optional.of(keyword));

		keywordService.delete(USER_ID, 10L);

		assertThat(keyword.getStatus()).isEqualTo(KeywordStatus.DELETED);
	}

	@Test
	void listFiltersByCategoryAndAnalysisStatus() {
		PageRequest pageable = PageRequest.of(0, 20);
		when(keywordRepository.findByUserIdAndStatusAndCategoryCodeAndAnalysisStatus(
				USER_ID,
				KeywordStatus.ACTIVE,
				CategoryCode.CAR_ACCESSORY,
				AnalysisStatus.PENDING,
				pageable
		)).thenReturn(Page.empty(pageable));

		keywordService.list(USER_ID, CategoryCode.CAR_ACCESSORY, AnalysisStatus.PENDING, pageable);

		verify(keywordRepository).findByUserIdAndStatusAndCategoryCodeAndAnalysisStatus(
				USER_ID,
				KeywordStatus.ACTIVE,
				CategoryCode.CAR_ACCESSORY,
				AnalysisStatus.PENDING,
				pageable
		);
	}
}
