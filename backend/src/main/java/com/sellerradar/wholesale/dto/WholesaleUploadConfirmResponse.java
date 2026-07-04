package com.sellerradar.wholesale.dto;

import java.util.List;

public record WholesaleUploadConfirmResponse(
		Long uploadId,
		int successCount,
		int failureCount,
		List<FailureReason> failureReasons
) {
	public record FailureReason(
			int rowNo,
			String message
	) {
	}
}
