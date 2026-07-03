package com.sellerradar.candidate.repository;

import com.sellerradar.candidate.domain.ProductCandidate;
import com.sellerradar.candidate.domain.CandidateStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductCandidateRepository extends JpaRepository<ProductCandidate, Long>, JpaSpecificationExecutor<ProductCandidate> {
	Optional<ProductCandidate> findByIdAndUserId(Long id, Long userId);

	boolean existsByWholesaleProductId(Long wholesaleProductId);

	List<ProductCandidate> findByUserIdAndStatusNot(Long userId, CandidateStatus status);
}
