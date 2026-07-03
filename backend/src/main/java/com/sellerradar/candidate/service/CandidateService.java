package com.sellerradar.candidate.service;

import com.sellerradar.candidate.domain.CandidateScore;
import com.sellerradar.candidate.domain.CandidateSourceType;
import com.sellerradar.candidate.domain.CandidateStatus;
import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.dto.CandidateDetailResponse;
import com.sellerradar.candidate.dto.CandidateListResponse;
import com.sellerradar.candidate.repository.ProductCandidateRepository;
import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.scoring.CandidateGrade;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateService {
	private final ProductCandidateRepository candidateRepository;

	public CandidateService(ProductCandidateRepository candidateRepository) {
		this.candidateRepository = candidateRepository;
	}

	@Transactional(readOnly = true)
	public Page<CandidateListResponse> list(
			Long userId,
			CandidateGrade grade,
			CategoryCode categoryCode,
			Integer minScore,
			BigDecimal minMarginRate,
			CandidateSourceType source,
			Pageable pageable
	) {
		return candidateRepository.findAll(
						filters(userId, grade, categoryCode, minScore, minMarginRate, source),
						pageable
				)
				.map(CandidateListResponse::from);
	}

	@Transactional(readOnly = true)
	public CandidateDetailResponse get(Long userId, Long candidateId) {
		return CandidateDetailResponse.from(getCandidate(userId, candidateId));
	}

	@Transactional
	public CandidateDetailResponse save(Long userId, Long candidateId) {
		ProductCandidate candidate = getCandidate(userId, candidateId);
		candidate.saveInterest();
		return CandidateDetailResponse.from(candidate);
	}

	@Transactional
	public CandidateDetailResponse exclude(Long userId, Long candidateId) {
		ProductCandidate candidate = getCandidate(userId, candidateId);
		candidate.exclude();
		return CandidateDetailResponse.from(candidate);
	}

	private ProductCandidate getCandidate(Long userId, Long candidateId) {
		return candidateRepository.findByIdAndUserId(candidateId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CANDIDATE_NOT_FOUND));
	}

	private Specification<ProductCandidate> filters(
			Long userId,
			CandidateGrade grade,
			CategoryCode categoryCode,
			Integer minScore,
			BigDecimal minMarginRate,
			CandidateSourceType source
	) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
			predicates.add(criteriaBuilder.notEqual(root.get("status"), CandidateStatus.EXCLUDED));
			if (grade != null) {
				predicates.add(criteriaBuilder.equal(root.get("grade"), grade));
			}
			if (categoryCode != null) {
				predicates.add(criteriaBuilder.equal(root.get("categoryCode"), categoryCode));
			}
			if (minMarginRate != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("expectedMarginRate"), minMarginRate));
			}
			if (source != null) {
				predicates.add(criteriaBuilder.equal(root.get("sourceType"), source));
			}
			if (minScore != null) {
				Join<ProductCandidate, CandidateScore> score = root.join("score", JoinType.INNER);
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(score.get("overallScore"), minScore));
			}
			return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
		};
	}
}
