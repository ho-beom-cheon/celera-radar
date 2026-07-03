package com.sellerradar.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "category_master")
public class CategoryMaster {
	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "code", nullable = false, length = 50)
	private CategoryCode code;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected CategoryMaster() {
	}

	private CategoryMaster(CategoryCode code, int sortOrder) {
		this.code = code;
		this.displayName = code.displayName();
		this.sortOrder = sortOrder;
		this.active = true;
	}

	public static CategoryMaster active(CategoryCode code, int sortOrder) {
		return new CategoryMaster(code, sortOrder);
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

	public CategoryCode getCode() {
		return code;
	}

	public String getDisplayName() {
		return displayName;
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
