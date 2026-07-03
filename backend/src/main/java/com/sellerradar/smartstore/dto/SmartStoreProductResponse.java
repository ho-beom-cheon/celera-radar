package com.sellerradar.smartstore.dto;

import com.sellerradar.smartstore.domain.SmartStoreProduct;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SmartStoreProductResponse(
		Long productId,
		Long connectionId,
		String sourceProductId,
		String originProductNo,
		String productName,
		BigDecimal salePrice,
		String saleStatus,
		String imageUrl,
		String categoryName,
		OffsetDateTime lastSyncedAt
) {
	public static SmartStoreProductResponse from(SmartStoreProduct product) {
		return new SmartStoreProductResponse(
				product.getId(),
				product.getConnection().getId(),
				product.getSourceProductId(),
				product.getOriginProductNo(),
				product.getProductName(),
				product.getSalePrice(),
				product.getSaleStatus(),
				product.getImageUrl(),
				product.getCategoryName(),
				product.getLastSyncedAt()
		);
	}
}
