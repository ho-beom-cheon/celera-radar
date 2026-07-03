package com.sellerradar.keyword.dto;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.KeywordPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KeywordCreateRequest(
		@NotBlank(message = "키워드는 필수입니다.")
		@Size(min = 2, max = 50, message = "키워드는 2자 이상 50자 이하여야 합니다.")
		String keyword,

		@Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
		String category
) {
	public KeywordCreateRequest(String keyword, CategoryCode categoryCode, KeywordPriority ignoredPriority) {
		this(keyword, categoryCode == null ? null : categoryCode.name());
	}

	public String resolvedCategory() {
		return normalizeCategory(category);
	}

	static String normalizeCategory(String category) {
		if (category == null) {
			return null;
		}
		String trimmedCategory = category.trim();
		return trimmedCategory.isEmpty() ? null : trimmedCategory;
	}
}
