package com.sellerradar.keyword.repository;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
	boolean existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(Long userId, String normalizedKeyword);

	long countByUserIdAndActiveTrueAndDeletedAtIsNull(Long userId);

	List<Keyword> findByActiveTrueAndDeletedAtIsNull();

	Optional<Keyword> findByIdAndUserId(Long id, Long userId);

	Page<Keyword> findByUserIdAndActiveTrueAndDeletedAtIsNull(Long userId, Pageable pageable);

	Page<Keyword> findByUserIdAndActiveTrueAndCategoryAndDeletedAtIsNull(
			Long userId,
			String category,
			Pageable pageable
	);

	Page<Keyword> findByUserIdAndActiveTrueAndAnalysisStatusAndDeletedAtIsNull(
			Long userId,
			AnalysisStatus analysisStatus,
			Pageable pageable
	);

	Page<Keyword> findByUserIdAndActiveTrueAndCategoryAndAnalysisStatusAndDeletedAtIsNull(
			Long userId,
			String category,
			AnalysisStatus analysisStatus,
			Pageable pageable
	);

	default boolean existsByUserIdAndNormalizedKeywordAndStatus(
			Long userId,
			String normalizedKeyword,
			KeywordStatus status
	) {
		return status == KeywordStatus.ACTIVE
				&& existsByUserIdAndNormalizedKeywordAndActiveTrueAndDeletedAtIsNull(userId, normalizedKeyword);
	}

	default long countByUserIdAndStatus(Long userId, KeywordStatus status) {
		if (status != KeywordStatus.ACTIVE) {
			return 0;
		}
		return countByUserIdAndActiveTrueAndDeletedAtIsNull(userId);
	}

	default List<Keyword> findByStatus(KeywordStatus status) {
		if (status != KeywordStatus.ACTIVE) {
			return List.of();
		}
		return findByActiveTrueAndDeletedAtIsNull();
	}

	default Page<Keyword> findByUserIdAndStatus(Long userId, KeywordStatus status, Pageable pageable) {
		if (status != KeywordStatus.ACTIVE) {
			return Page.empty(pageable);
		}
		return findByUserIdAndActiveTrueAndDeletedAtIsNull(userId, pageable);
	}

	default Page<Keyword> findByUserIdAndStatusAndCategoryCode(
			Long userId,
			KeywordStatus status,
			CategoryCode categoryCode,
			Pageable pageable
	) {
		if (status != KeywordStatus.ACTIVE) {
			return Page.empty(pageable);
		}
		return findByUserIdAndActiveTrueAndCategoryAndDeletedAtIsNull(userId, categoryCode.name(), pageable);
	}

	default Page<Keyword> findByUserIdAndStatusAndAnalysisStatus(
			Long userId,
			KeywordStatus status,
			AnalysisStatus analysisStatus,
			Pageable pageable
	) {
		if (status != KeywordStatus.ACTIVE) {
			return Page.empty(pageable);
		}
		return findByUserIdAndActiveTrueAndAnalysisStatusAndDeletedAtIsNull(userId, analysisStatus, pageable);
	}

	default Page<Keyword> findByUserIdAndStatusAndCategoryCodeAndAnalysisStatus(
			Long userId,
			KeywordStatus status,
			CategoryCode categoryCode,
			AnalysisStatus analysisStatus,
			Pageable pageable
	) {
		if (status != KeywordStatus.ACTIVE) {
			return Page.empty(pageable);
		}
		return findByUserIdAndActiveTrueAndCategoryAndAnalysisStatusAndDeletedAtIsNull(
				userId,
				categoryCode.name(),
				analysisStatus,
				pageable
		);
	}
}
