package com.sellerradar.alert.dto;

import com.sellerradar.alert.domain.AlertFrequency;
import com.sellerradar.alert.domain.AlertRule;
import com.sellerradar.category.domain.CategoryCode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AlertRuleResponse(
		Long id,
		String name,
		int minScore,
		BigDecimal minMarginRate,
		List<CategoryCode> categoryCodes,
		boolean riskExcluded,
		AlertFrequency frequency,
		boolean active,
		OffsetDateTime createdAt
) {
	public static AlertRuleResponse from(AlertRule rule) {
		return new AlertRuleResponse(
				rule.getId(),
				rule.getName(),
				rule.getMinScore(),
				rule.getMinMarginRate(),
				rule.categoryCodes(),
				rule.isRiskExcluded(),
				rule.getFrequency(),
				rule.isActive(),
				rule.getCreatedAt()
		);
	}
}
