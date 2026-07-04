package com.sellerradar.wholesale.dto;

import java.math.BigDecimal;

public record WholesaleFilePreviewCellResponse(
		String header,
		String rawValue,
		BigDecimal decimalValue,
		Long longValue
) {
}
