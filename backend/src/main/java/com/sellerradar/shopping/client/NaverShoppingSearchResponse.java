package com.sellerradar.shopping.client;

import java.util.List;

public record NaverShoppingSearchResponse(
		String lastBuildDate,
		long total,
		int start,
		int display,
		List<NaverShoppingSearchItem> items
) {
}
