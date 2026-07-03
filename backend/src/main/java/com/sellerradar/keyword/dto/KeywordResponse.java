package com.sellerradar.keyword.dto;

import com.sellerradar.keyword.domain.AnalysisStatus;
import com.sellerradar.keyword.domain.Keyword;
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
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public static KeywordResponse from(Keyword keyword) {
		return new KeywordResponse(
				keyword.getId(),
				keyword.getKeyword(),
				keyword.getCategory(),
				keyword.isActive(),
				keyword.getAnalysisStatus(),
				keyword.getLastAnalyzedAt(),
				keyword.getLastSnapshotDate(),
				keyword.getCreatedAt(),
				keyword.getUpdatedAt()
		);
	}
}
