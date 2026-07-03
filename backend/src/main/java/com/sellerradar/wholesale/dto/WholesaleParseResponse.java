package com.sellerradar.wholesale.dto;

public record WholesaleParseResponse(
		Long fileId,
		int parsedCount,
		int invalidCount
) {
}
