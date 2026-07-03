package com.sellerradar.keyword.dto;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.KeywordPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KeywordCreateRequest(
		@NotBlank(message = "키워드는 필수입니다.")
		@Size(min = 2, max = 50, message = "키워드는 2자 이상 50자 이하여야 합니다.")
		String keyword,

		@NotNull(message = "카테고리는 필수입니다.")
		CategoryCode categoryCode,

		KeywordPriority priority
) {
	public KeywordPriority resolvedPriority() {
		return priority == null ? KeywordPriority.MEDIUM : priority;
	}
}
