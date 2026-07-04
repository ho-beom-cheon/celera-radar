package com.sellerradar.smartstore.client;

import java.math.BigDecimal;

public record SmartStoreProductSyncItem(
		String sourceProductId,
		String originProductNo,
		String productName,
		BigDecimal salePrice,
		String saleStatus,
		String imageUrl,
		String categoryName,
		String rawPayload
) {
}
