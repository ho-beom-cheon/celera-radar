package com.sellerradar.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "risk_category_rule")
public class RiskCategoryRule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "risk_keyword", nullable = false, unique = true, length = 100)
	private String riskKeyword;

	@Enumerated(EnumType.STRING)
	@Column(name = "handling_type", nullable = false, length = 20)
	private RiskHandlingType handlingType;

	@Column(nullable = false, length = 255)
	private String reason;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected RiskCategoryRule() {
	}

	private RiskCategoryRule(String riskKeyword, RiskHandlingType handlingType, String reason, int sortOrder) {
		this.riskKeyword = riskKeyword;
		this.handlingType = handlingType;
		this.reason = reason;
		this.sortOrder = sortOrder;
		this.active = true;
	}

	public static RiskCategoryRule active(
			String riskKeyword,
			RiskHandlingType handlingType,
			String reason,
			int sortOrder
	) {
		return new RiskCategoryRule(riskKeyword, handlingType, reason, sortOrder);
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getRiskKeyword() {
		return riskKeyword;
	}

	public RiskHandlingType getHandlingType() {
		return handlingType;
	}

	public String getReason() {
		return reason;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public boolean isActive() {
		return active;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
