package com.sellerradar.smartstore.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StoreProductCostResponse(
		Long id,
		Long storeProductId,
		Long wholesaleProductId,
		String wholesaleProductName,
		BigDecimal purchaseCost,
		BigDecimal shippingFee,
		BigDecimal packagingFee,
		BigDecimal extraCost,
		BigDecimal platformFeeRate,
		BigDecimal targetMarginRate,
		BigDecimal salePrice,
		int totalCost,
		int expectedProfit,
		BigDecimal expectedMarginRate,
		int recommendedSalePrice,
		String memo,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}
