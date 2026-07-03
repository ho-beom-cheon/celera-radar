package com.sellerradar.batch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "batch_job_history")
public class BatchJobHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "job_type", nullable = false, length = 50)
	private BatchJobType jobType;

	@Enumerated(EnumType.STRING)
	@Column(name = "trigger_type", nullable = false, length = 20)
	private BatchTriggerType triggerType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BatchJobStatus status;

	@Column(name = "target_count", nullable = false)
	private int targetCount;

	@Column(name = "success_count", nullable = false)
	private int successCount;

	@Column(name = "failure_count", nullable = false)
	private int failureCount;

	@Column(name = "started_at", nullable = false)
	private OffsetDateTime startedAt;

	@Column(name = "finished_at")
	private OffsetDateTime finishedAt;

	@Column(name = "error_message", length = 500)
	private String errorMessage;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected BatchJobHistory() {
	}

	private BatchJobHistory(
			BatchJobType jobType,
			BatchTriggerType triggerType,
			int targetCount,
			OffsetDateTime startedAt
	) {
		this.jobType = jobType;
		this.triggerType = triggerType;
		this.status = BatchJobStatus.RUNNING;
		this.targetCount = targetCount;
		this.startedAt = startedAt;
	}

	public static BatchJobHistory start(
			BatchJobType jobType,
			BatchTriggerType triggerType,
			int targetCount,
			OffsetDateTime startedAt
	) {
		return new BatchJobHistory(jobType, triggerType, targetCount, startedAt);
	}

	public void complete(int successCount, int failureCount, OffsetDateTime finishedAt) {
		this.successCount = successCount;
		this.failureCount = failureCount;
		this.finishedAt = finishedAt;
		this.status = resolveCompletedStatus(successCount, failureCount);
		if (failureCount > 0) {
			this.errorMessage = "failedKeywords=" + failureCount;
		}
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	private BatchJobStatus resolveCompletedStatus(int successCount, int failureCount) {
		if (failureCount == 0) {
			return BatchJobStatus.SUCCESS;
		}
		if (successCount == 0) {
			return BatchJobStatus.FAILED;
		}
		return BatchJobStatus.PARTIAL_SUCCESS;
	}

	public Long getId() {
		return id;
	}

	public BatchJobType getJobType() {
		return jobType;
	}

	public BatchTriggerType getTriggerType() {
		return triggerType;
	}

	public BatchJobStatus getStatus() {
		return status;
	}

	public int getTargetCount() {
		return targetCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public OffsetDateTime getFinishedAt() {
		return finishedAt;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
