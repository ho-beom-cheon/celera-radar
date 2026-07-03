package com.sellerradar.category.repository;

import com.sellerradar.category.domain.RiskCategoryRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskCategoryRuleRepository extends JpaRepository<RiskCategoryRule, Long> {
	boolean existsByRiskKeyword(String riskKeyword);

	List<RiskCategoryRule> findByActiveTrueOrderBySortOrderAsc();
}
