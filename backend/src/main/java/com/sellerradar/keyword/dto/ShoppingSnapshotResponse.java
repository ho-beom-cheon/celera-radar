package com.sellerradar.keyword.dto;

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
		OffsetDateTime fetchedAt,
		List<ShoppingSnapshotItemResponse> topItems
) {
}
