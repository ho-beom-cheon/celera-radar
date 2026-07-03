package com.sellerradar.keyword.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeywordService {
	private final KeywordRepository keywordRepository;
	private final UserRepository userRepository;
	private final KeywordNormalizer keywordNormalizer;

	public KeywordService(
			KeywordRepository keywordRepository,
			UserRepository userRepository,
			KeywordNormalizer keywordNormalizer
	) {
		this.keywordRepository = keywordRepository;
		this.userRepository = userRepository;
		this.keywordNormalizer = keywordNormalizer;
	}

	@Transactional(readOnly = true)
	public Page<KeywordResponse> list(
			Long userId,
			CategoryCode categoryCode,
			AnalysisStatus analysisStatus,
			Pageable pageable
	) {
		Page<Keyword> keywords = findActiveKeywords(userId, categoryCode, analysisStatus, pageable);
		return keywords.map(KeywordResponse::from);
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
				request.categoryCode(),
				request.resolvedPriority()
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
		KeywordPriority priority = request.resolvedPriority();
		keyword.update(displayKeyword, normalizedKeyword, request.categoryCode(), priority);
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
			CategoryCode categoryCode,
			AnalysisStatus analysisStatus,
			Pageable pageable
	) {
		if (categoryCode != null && analysisStatus != null) {
			return keywordRepository.findByUserIdAndStatusAndCategoryCodeAndAnalysisStatus(
					userId,
					KeywordStatus.ACTIVE,
					categoryCode,
					analysisStatus,
					pageable
			);
		}
		if (categoryCode != null) {
			return keywordRepository.findByUserIdAndStatusAndCategoryCode(
					userId,
					KeywordStatus.ACTIVE,
					categoryCode,
					pageable
			);
		}
		if (analysisStatus != null) {
			return keywordRepository.findByUserIdAndStatusAndAnalysisStatus(
					userId,
					KeywordStatus.ACTIVE,
					analysisStatus,
					pageable
			);
		}
		return keywordRepository.findByUserIdAndStatus(userId, KeywordStatus.ACTIVE, pageable);
	}

	private Keyword getActiveKeyword(Long userId, Long keywordId) {
		Keyword keyword = keywordRepository.findByIdAndUserId(keywordId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.KEYWORD_NOT_FOUND));
		if (keyword.getStatus() == KeywordStatus.DELETED) {
			throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
		}
		return keyword;
	}

	private void validateDuplicatedKeyword(Long userId, String normalizedKeyword) {
		if (keywordRepository.existsByUserIdAndNormalizedKeywordAndStatus(
				userId,
				normalizedKeyword,
				KeywordStatus.ACTIVE
		)) {
			throw new BusinessException(ErrorCode.DUPLICATED_KEYWORD, ErrorCode.DUPLICATED_KEYWORD.defaultMessage(), "keyword");
		}
	}

	private void validateKeywordLimit(User user) {
		long activeKeywordCount = keywordRepository.countByUserIdAndStatus(user.getId(), KeywordStatus.ACTIVE);
		if (activeKeywordCount >= user.getPlanCode().keywordLimit()) {
			throw new BusinessException(
					ErrorCode.KEYWORD_LIMIT_EXCEEDED,
					ErrorCode.KEYWORD_LIMIT_EXCEEDED.defaultMessage(),
					"keyword"
			);
		}
	}
}
