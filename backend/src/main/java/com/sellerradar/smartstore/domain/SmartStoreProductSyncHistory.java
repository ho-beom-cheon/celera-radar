package com.sellerradar.smartstore.domain;

import com.sellerradar.batch.domain.BatchJobStatus;
import com.sellerradar.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "naver_store_product_sync_histories")
public class SmartStoreProductSyncHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "connection_id", nullable = false)
	private SmartStoreConnection connection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
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

	protected SmartStoreProductSyncHistory() {
	}

	private SmartStoreProductSyncHistory(SmartStoreConnection connection, OffsetDateTime startedAt) {
		this.connection = connection;
		this.user = connection.getUser();
		this.status = BatchJobStatus.RUNNING;
		this.startedAt = startedAt;
	}

	public static SmartStoreProductSyncHistory start(SmartStoreConnection connection, OffsetDateTime startedAt) {
		return new SmartStoreProductSyncHistory(connection, startedAt);
	}

	public void complete(int targetCount, int successCount, int failureCount, OffsetDateTime finishedAt) {
		this.targetCount = Math.max(targetCount, 0);
		this.successCount = Math.max(successCount, 0);
		this.failureCount = Math.max(failureCount, 0);
		this.finishedAt = finishedAt;
		if (this.failureCount == 0) {
			this.status = BatchJobStatus.SUCCESS;
		} else if (this.successCount == 0) {
			this.status = BatchJobStatus.FAILED;
		} else {
			this.status = BatchJobStatus.PARTIAL_SUCCESS;
		}
	}

	public void fail(String errorMessage, OffsetDateTime finishedAt) {
		this.status = BatchJobStatus.FAILED;
		this.failureCount = 1;
		this.finishedAt = finishedAt;
		this.errorMessage = truncate(errorMessage);
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	private String truncate(String value) {
		if (value == null || value.isBlank()) {
			return "smartstore product sync failed";
		}
		return value.length() <= 500 ? value : value.substring(0, 500);
	}

	public Long getId() {
		return id;
	}

	public SmartStoreConnection getConnection() {
		return connection;
	}

	public User getUser() {
		return user;
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
}
