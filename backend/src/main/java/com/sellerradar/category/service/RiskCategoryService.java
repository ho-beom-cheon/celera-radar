package com.sellerradar.category.service;

import com.sellerradar.category.domain.RiskCategoryRule;
import com.sellerradar.category.repository.RiskCategoryRuleRepository;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskCategoryService {
	private final RiskCategoryRuleRepository riskCategoryRuleRepository;

	public RiskCategoryService(RiskCategoryRuleRepository riskCategoryRuleRepository) {
		this.riskCategoryRuleRepository = riskCategoryRuleRepository;
	}

	@Transactional(readOnly = true)
	public RiskCategoryDecision evaluate(String sourceCategory) {
		if (sourceCategory == null || sourceCategory.isBlank()) {
			return RiskCategoryDecision.safe();
		}
		String normalizedSourceCategory = normalize(sourceCategory);
		return riskCategoryRuleRepository.findByActiveTrueOrderBySortOrderAsc()
				.stream()
				.sorted(Comparator
						.comparingInt((RiskCategoryRule rule) -> normalize(rule.getRiskKeyword()).length())
						.reversed()
						.thenComparingInt(RiskCategoryRule::getSortOrder))
				.filter(rule -> normalizedSourceCategory.contains(normalize(rule.getRiskKeyword())))
				.findFirst()
				.map(rule -> RiskCategoryDecision.matched(
						rule.getHandlingType(),
						rule.getRiskKeyword(),
						rule.getReason()
				))
				.orElseGet(RiskCategoryDecision::safe);
	}

	private String normalize(String value) {
		return value.trim().toLowerCase().replaceAll("\\s+", "");
	}
}
