package com.sellerradar.smartstore.dto;

import com.sellerradar.batch.domain.BatchJobStatus;
import java.time.OffsetDateTime;

public record SmartStoreProductSyncResponse(
		Long historyId,
		Long connectionId,
		BatchJobStatus status,
		int targetCount,
		int successCount,
		int failureCount,
		OffsetDateTime syncedAt
) {
}
