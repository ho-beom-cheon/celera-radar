package com.sellerradar.wholesale.dto;

import com.sellerradar.wholesale.domain.WholesaleProduct;
import com.sellerradar.wholesale.domain.WholesaleProductParseStatus;

public record WholesaleProductRowResponse(
		Long id,
		int rowNo,
		String productName,
		Integer supplyPrice,
		Integer shippingFee,
		String sourceCategory,
		String productUrl,
		WholesaleProductParseStatus parseStatus,
		String errorMessage
) {
	public static WholesaleProductRowResponse from(WholesaleProduct product) {
		return new WholesaleProductRowResponse(
				product.getId(),
				product.getRowNo(),
				product.getProductName(),
				product.getSupplyPrice(),
				product.getShippingFee(),
				product.getSourceCategory(),
				product.getProductUrl(),
				product.getParseStatus(),
				product.getErrorMessage()
		);
	}
}
