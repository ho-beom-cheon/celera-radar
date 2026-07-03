package com.sellerradar.keyword.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeywordService {
	private final KeywordRepository keywordRepository;
	private final UserRepository userRepository;
	private final KeywordNormalizer keywordNormalizer;
	private final ShoppingPriceSnapshotRepository snapshotRepository;

	public KeywordService(
			KeywordRepository keywordRepository,
			UserRepository userRepository,
			KeywordNormalizer keywordNormalizer,
			ShoppingPriceSnapshotRepository snapshotRepository
	) {
		this.keywordRepository = keywordRepository;
		this.userRepository = userRepository;
		this.keywordNormalizer = keywordNormalizer;
		this.snapshotRepository = snapshotRepository;
	}

	@Transactional(readOnly = true)
	public Page<KeywordResponse> list(
			Long userId,
			String category,
			AnalysisStatus analysisStatus,
			Pageable pageable
	) {
		Page<Keyword> keywords = findActiveKeywords(userId, normalizeCategory(category), analysisStatus, pageable);
		return keywords.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public KeywordResponse get(Long userId, Long keywordId) {
		return toResponse(getActiveKeyword(userId, keywordId));
	}

	@Transactional
	public KeywordResponse create(Long userId, KeywordCreateRequest request) {
		User user = getUser(userId);
		String displayKeyword = keywordNormalizer.displayKeyword(request.keyword());
		String normalizedKeyword = keywordNormalizer.normalize(request.keyword());
		validateDuplicatedKeyword(userId, normalizedKeyword);
		validateKeywordLimit(user);
		Keyword keyword = Keyword.create(
				user,
				displayKeyword,
				normalizedKeyword,
				request.resolvedCategory()
		);
		return KeywordResponse.from(keywordRepository.save(keyword));
	}

	@Transactional
	public KeywordResponse update(Long userId, Long keywordId, KeywordUpdateRequest request) {
		Keyword keyword = getActiveKeyword(userId, keywordId);
		String displayKeyword = keywordNormalizer.displayKeyword(request.keyword());
		String normalizedKeyword = keywordNormalizer.normalize(request.keyword());
		if (!keyword.getNormalizedKeyword().equals(normalizedKeyword)) {
			validateDuplicatedKeyword(userId, normalizedKeyword);
		}
		keyword.update(displayKeyword, normalizedKeyword, request.resolvedCategory());
		return KeywordResponse.from(keyword);
	}

	@Transactional
	public void delete(Long userId, Long keywordId) {
		Keyword keyword = getActiveKeyword(userId, keywordId);
		keyword.delete();
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	private Page<Keyword> findActiveKeywords(
			Long userId,
			String category,
			AnalysisStatus analysisStatus,
			Pageable pageable
	) {
		if (category != null && analysisStatus != null) {
			return keywordRepository.findByUserIdAndActiveTrueAndCategoryAndAnalysisStatusAndDeletedAtIsNull(
					userId,
					category,
					analysisStatus,
					pageable
			);
		}
		if (category != null) {
			return keywordRepository.findByUserIdAndActiveTrueAndCategoryAndDeletedAtIsNull(
					userId,
					category,
					pageable
			);
		}
		if (analysisStatus != null) {
			return keywordRepository.findByUserIdAndActiveTrueAndAnalysisStatusAndDeletedAtIsNull(
					userId,
					analysisStatus,
					pageable
			);
		}
		return keywordRepository.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId, pageable);
	}

	private Keyword getActiveKeyword(Long userId, Long keywordId) {
		return keywordRepository.findByIdAndUserIdAndActiveTrueAndDeletedAtIsNull(keywordId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
	}

	private void validateDuplicatedKeyword(Long userId, String normalizedKeyword) {
		if (keywordRepository.existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(
				userId,
				normalizedKeyword
		)) {
			throw new BusinessException(ErrorCode.DUPLICATED_KEYWORD, ErrorCode.DUPLICATED_KEYWORD.defaultMessage(), "keyword");
		}
	}

	private void validateKeywordLimit(User user) {
		long activeKeywordCount = keywordRepository.countByUserIdAndActiveTrueAndDeletedAtIsNull(user.getId());
		if (activeKeywordCount >= user.getPlanCode().keywordLimit()) {
			throw new BusinessException(
					ErrorCode.KEYWORD_LIMIT_EXCEEDED,
					ErrorCode.KEYWORD_LIMIT_EXCEEDED.defaultMessage(),
					"keyword"
			);
		}
	}

	private String normalizeCategory(String category) {
		if (category == null) {
			return null;
		}
		String trimmedCategory = category.trim();
		return trimmedCategory.isEmpty() ? null : trimmedCategory;
	}

	private KeywordResponse toResponse(Keyword keyword) {
		return KeywordResponse.from(
				keyword,
				snapshotRepository.findFirstByKeyword_IdOrderBySearchDateDesc(keyword.getId()).orElse(null)
		);
	}
}
