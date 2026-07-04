package com.sellerradar.smartstore.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StoreProductCostUpsertRequest(
		Long wholesaleProductId,
		@DecimalMin("0.0") BigDecimal purchaseCost,
		@DecimalMin("0.0") BigDecimal shippingFee,
		@DecimalMin("0.0") BigDecimal packagingFee,
		@DecimalMin("0.0") BigDecimal extraCost,
		@DecimalMin("0.0") @DecimalMax("99.0") BigDecimal platformFeeRate,
		@DecimalMin("0.0") @DecimalMax("99.0") BigDecimal targetMarginRate,
		@Size(max = 1000) String memo
) {
}
