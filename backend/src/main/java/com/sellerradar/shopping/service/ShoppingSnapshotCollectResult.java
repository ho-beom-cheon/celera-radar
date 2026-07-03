package com.sellerradar.shopping.service;

import com.sellerradar.shopping.domain.ShoppingPriceSnapshot;

public record ShoppingSnapshotCollectResult(
		ShoppingPriceSnapshot snapshot,
		boolean cached
) {
}
