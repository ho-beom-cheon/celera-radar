package com.sellerradar.keyword.dto;

import com.sellerradar.shopping.domain.ShoppingCompetitionLevel;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ShoppingSnapshotResponse(
		Long keywordId,
		String keyword,
		LocalDate searchDate,
		String sortType,
		boolean cached,
		Integer totalCount,
		Integer minPrice,
		Integer maxPrice,
		Integer avgPrice,
		ShoppingCompetitionLevel competitionLevel,
		OffsetDateTime fetchedAt,
		List<ShoppingSnapshotItemResponse> topItems
) {
}
