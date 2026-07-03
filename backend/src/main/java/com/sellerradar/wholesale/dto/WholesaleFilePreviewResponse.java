package com.sellerradar.wholesale.dto;

import java.util.List;

public record WholesaleFilePreviewResponse(
		String originalFilename,
		String fileType,
		List<String> headers,
		List<WholesaleFilePreviewRowResponse> rows,
		int rowCount
) {
}
