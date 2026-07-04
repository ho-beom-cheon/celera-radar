package com.sellerradar.wholesale.dto;

import java.util.List;

public record WholesaleFilePreviewRowResponse(
		int rowNo,
		List<WholesaleFilePreviewCellResponse> cells
) {
}
