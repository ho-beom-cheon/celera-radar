package com.sellerradar.alert.domain;

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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "alert_rule")
public class AlertRule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "min_score", nullable = false)
	private int minScore;

	@Column(name = "min_margin_rate", nullable = false, precision = 6, scale = 2)
	private BigDecimal minMarginRate;

	@Column(name = "category_codes", nullable = false, columnDefinition = "TEXT")
	private String categoryCodes;

	@Column(name = "risk_excluded", nullable = false)
	private boolean riskExcluded;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AlertFrequency frequency;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected AlertRule() {
	}

	private AlertRule(
			User user,
			String name,
			int minScore,
			BigDecimal minMarginRate,
			List<CategoryCode> categoryCodes,
			boolean riskExcluded,
			AlertFrequency frequency
	) {
		this.user = user;
		this.name = name;
		this.minScore = minScore;
		this.minMarginRate = minMarginRate;
		this.categoryCodes = joinCategoryCodes(categoryCodes);
		this.riskExcluded = riskExcluded;
		this.frequency = frequency;
		this.active = true;
	}

	public static AlertRule create(
			User user,
			String name,
			int minScore,
			BigDecimal minMarginRate,
			List<CategoryCode> categoryCodes,
			boolean riskExcluded,
			AlertFrequency frequency
	) {
		return new AlertRule(user, name, minScore, minMarginRate, categoryCodes, riskExcluded, frequency);
	}

	public List<CategoryCode> categoryCodes() {
		if (categoryCodes == null || categoryCodes.isBlank()) {
			return List.of();
		}
		return categoryCodes.lines()
				.map(String::strip)
				.filter(value -> !value.isBlank())
				.map(CategoryCode::valueOf)
				.toList();
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

	private static String joinCategoryCodes(List<CategoryCode> codes) {
		if (codes == null || codes.isEmpty()) {
			return "";
		}
		return String.join("\n", codes.stream().map(Enum::name).toList());
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getName() {
		return name;
	}

	public int getMinScore() {
		return minScore;
	}

	public BigDecimal getMinMarginRate() {
		return minMarginRate;
	}

	public boolean isRiskExcluded() {
		return riskExcluded;
	}

	public AlertFrequency getFrequency() {
		return frequency;
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
