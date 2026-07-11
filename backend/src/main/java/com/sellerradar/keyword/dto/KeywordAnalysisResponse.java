package com.sellerradar.keyword.dto;

import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.trend.dto.TrendAnalysisResponse;
import java.time.OffsetDateTime;

public record KeywordAnalysisResponse(
		Long keywordId,
		String keyword,
		AnalysisStatus status,
		OffsetDateTime lastAnalyzedAt,
		ShoppingAnalysisResponse shopping,
		TrendAnalysisResponse trend,
		Object score
) {
}
