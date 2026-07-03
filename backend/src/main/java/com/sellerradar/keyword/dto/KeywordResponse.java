package com.sellerradar.keyword.dto;

import com.sellerradar.category.domain.CategoryCode;
import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.keyword.domain.KeywordPriority;
import java.time.OffsetDateTime;

public record KeywordResponse(
		Long id,
		String keyword,
		CategoryCode categoryCode,
		KeywordPriority priority,
		AnalysisStatus analysisStatus,
		OffsetDateTime lastAnalyzedAt,
		OffsetDateTime createdAt
) {
	public static KeywordResponse from(Keyword keyword) {
		return new KeywordResponse(
				keyword.getId(),
				keyword.getKeyword(),
				keyword.getCategoryCode(),
				keyword.getPriority(),
				keyword.getAnalysisStatus(),
				keyword.getLastAnalyzedAt(),
				keyword.getCreatedAt()
		);
	}
}
