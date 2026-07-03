package com.sellerradar.alert.repository;

import com.sellerradar.alert.domain.Alert;
import com.sellerradar.alert.domain.AlertType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
	@EntityGraph(attributePaths = {"candidate", "rule"})
	Page<Alert> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<Alert> findByIdAndUserId(Long id, Long userId);

	boolean existsByRuleIdAndCandidateIdAndType(Long ruleId, Long candidateId, AlertType type);
}
