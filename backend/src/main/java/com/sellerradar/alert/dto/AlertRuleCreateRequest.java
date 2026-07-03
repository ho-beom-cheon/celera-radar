package com.sellerradar.alert.dto;

import com.sellerradar.alert.domain.AlertFrequency;
import com.sellerradar.category.domain.CategoryCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record AlertRuleCreateRequest(
		@NotBlank String name,
		@Min(0) @Max(100) int minScore,
		@NotNull @DecimalMin("0.0") BigDecimal minMarginRate,
		List<CategoryCode> categoryCodes,
		boolean riskExcluded,
		@NotNull AlertFrequency frequency
) {
}
