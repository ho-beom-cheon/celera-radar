package com.sellerradar.keyword.dto;

import com.sellerradar.keyword.domain.AnalysisStatus;
import java.time.OffsetDateTime;

public record KeywordAnalysisResponse(
		Long keywordId,
		String keyword,
		AnalysisStatus status,
		OffsetDateTime lastAnalyzedAt,
		ShoppingAnalysisResponse shopping,
		Object trend,
		Object score
) {
}
