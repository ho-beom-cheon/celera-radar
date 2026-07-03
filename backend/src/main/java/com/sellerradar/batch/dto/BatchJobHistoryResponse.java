package com.sellerradar.batch.dto;

import com.sellerradar.batch.domain.BatchJobHistory;
import com.sellerradar.batch.domain.BatchJobStatus;
import com.sellerradar.batch.domain.BatchJobType;
import com.sellerradar.batch.domain.BatchTriggerType;
import java.time.OffsetDateTime;

public record BatchJobHistoryResponse(
		Long id,
		BatchJobType jobType,
		BatchTriggerType triggerType,
		BatchJobStatus status,
		int targetCount,
		int successCount,
		int failureCount,
		OffsetDateTime startedAt,
		OffsetDateTime finishedAt,
		String errorMessage
) {
	public static BatchJobHistoryResponse from(BatchJobHistory history) {
		return new BatchJobHistoryResponse(
				history.getId(),
				history.getJobType(),
				history.getTriggerType(),
				history.getStatus(),
				history.getTargetCount(),
				history.getSuccessCount(),
				history.getFailureCount(),
				history.getStartedAt(),
				history.getFinishedAt(),
				history.getErrorMessage()
		);
	}
}
