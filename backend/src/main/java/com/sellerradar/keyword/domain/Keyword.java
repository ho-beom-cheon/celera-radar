package com.sellerradar.keyword.domain;

import com.sellerradar.category.domain.CategoryCode;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "keywords")
public class Keyword {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 200)
	private String keyword;

	@Column(name = "normalized_keyword", nullable = false, length = 200)
	private String normalizedKeyword;

	@Column(length = 100)
	private String category;

	@Transient
	private KeywordPriority priority;

	@Column(nullable = false)
	private boolean active;

	@Enumerated(EnumType.STRING)
	@Column(name = "analysis_status", nullable = false, length = 30)
	private AnalysisStatus analysisStatus;

	@Column(name = "last_analyzed_at")
	private OffsetDateTime lastAnalyzedAt;

	@Column(name = "last_snapshot_date")
	private LocalDate lastSnapshotDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected Keyword() {
	}

	private Keyword(User user, String keyword, String normalizedKeyword, String category, KeywordPriority priority) {
		this.user = user;
		this.keyword = keyword;
		this.normalizedKeyword = normalizedKeyword;
		this.category = normalizeCategory(category);
		this.priority = priority;
		this.active = true;
		this.analysisStatus = AnalysisStatus.PENDING;
	}

	public static Keyword create(
			User user,
			String keyword,
			String normalizedKeyword,
			String category
	) {
		return new Keyword(user, keyword, normalizedKeyword, category, KeywordPriority.MEDIUM);
	}

	public static Keyword create(
			User user,
			String keyword,
			String normalizedKeyword,
			CategoryCode categoryCode,
			KeywordPriority priority
	) {
		return new Keyword(
				user,
				keyword,
				normalizedKeyword,
				categoryCode == null ? null : categoryCode.name(),
				priority
		);
	}

	public void update(String keyword, String normalizedKeyword, String category) {
		this.keyword = keyword;
		this.normalizedKeyword = normalizedKeyword;
		this.category = normalizeCategory(category);
	}

	public void update(String keyword, String normalizedKeyword, CategoryCode categoryCode, KeywordPriority priority) {
		update(keyword, normalizedKeyword, categoryCode == null ? null : categoryCode.name());
		this.priority = priority;
	}

	public void delete() {
		this.active = false;
		this.deletedAt = OffsetDateTime.now();
	}

	public void markAnalyzed(OffsetDateTime analyzedAt) {
		this.analysisStatus = AnalysisStatus.SUCCESS;
		this.lastAnalyzedAt = analyzedAt;
	}

	public void markAnalysisFailed(OffsetDateTime analyzedAt) {
		this.analysisStatus = AnalysisStatus.FAILED;
		this.lastAnalyzedAt = analyzedAt;
	}

	public void markAnalysisSkipped(OffsetDateTime analyzedAt) {
		this.analysisStatus = AnalysisStatus.SKIPPED;
		this.lastAnalyzedAt = analyzedAt;
	}

	public void updateLastSnapshotDate(LocalDate lastSnapshotDate) {
		this.lastSnapshotDate = lastSnapshotDate;
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

	public User getUser() {
		return user;
	}

	public String getKeyword() {
		return keyword;
	}

	public String getNormalizedKeyword() {
		return normalizedKeyword;
	}

	public String getCategory() {
		return category;
	}

	public CategoryCode getCategoryCode() {
		if (category == null || category.isBlank()) {
			return null;
		}
		try {
			return CategoryCode.valueOf(category);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public KeywordPriority getPriority() {
		return priority == null ? KeywordPriority.MEDIUM : priority;
	}

	public KeywordStatus getStatus() {
		return active && deletedAt == null ? KeywordStatus.ACTIVE : KeywordStatus.DELETED;
	}

	public boolean isActive() {
		return active;
	}

	public AnalysisStatus getAnalysisStatus() {
		return analysisStatus;
	}

	public OffsetDateTime getLastAnalyzedAt() {
		return lastAnalyzedAt;
	}

	public LocalDate getLastSnapshotDate() {
		return lastSnapshotDate;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	private static String normalizeCategory(String category) {
		if (category == null) {
			return null;
		}
		String trimmedCategory = category.trim();
		return trimmedCategory.isEmpty() ? null : trimmedCategory;
	}
}
