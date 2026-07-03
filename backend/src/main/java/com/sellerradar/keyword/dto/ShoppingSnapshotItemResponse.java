package com.sellerradar.keyword.dto;

public record ShoppingSnapshotItemResponse(
		int rankNo,
		String title,
		String productUrl,
		String imageUrl,
		Integer lowPrice,
		String mallName,
		String category1,
		String category2,
		String category3,
		String category4
) {
}
