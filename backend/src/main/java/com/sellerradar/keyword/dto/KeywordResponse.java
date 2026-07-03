package com.sellerradar.keyword.dto;

import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
import com.sellerradar.shopping.domain.ShoppingCompetitionLevel;
import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record KeywordResponse(
		Long id,
		String keyword,
		String category,
		boolean active,
		AnalysisStatus analysisStatus,
		OffsetDateTime lastAnalyzedAt,
		LocalDate lastSnapshotDate,
		Integer latestMinPrice,
		Integer latestAvgPrice,
		ShoppingCompetitionLevel latestCompetitionLevel,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public static KeywordResponse from(Keyword keyword) {
		return from(keyword, null);
	}

	public static KeywordResponse from(Keyword keyword, ShoppingPriceSnapshot latestSnapshot) {
		return new KeywordResponse(
				keyword.getId(),
				keyword.getKeyword(),
				keyword.getCategory(),
				keyword.isActive(),
				keyword.getAnalysisStatus(),
				keyword.getLastAnalyzedAt(),
				keyword.getLastSnapshotDate(),
				latestSnapshot == null ? null : latestSnapshot.getMinPrice(),
				latestSnapshot == null ? null : latestSnapshot.getAvgPrice(),
				latestSnapshot == null ? null : latestSnapshot.getCompetitionLevel(),
				keyword.getCreatedAt(),
				keyword.getUpdatedAt()
		);
	}
}
