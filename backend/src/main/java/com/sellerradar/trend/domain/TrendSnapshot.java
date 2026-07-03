package com.sellerradar.trend.domain;

import com.sellerradar.keyword.domain.Keyword;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
		name = "trend_snapshot",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_trend_snapshot_keyword_period_time_unit",
				columnNames = {"keyword_id", "period", "time_unit"}
		)
)
public class TrendSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;

	@Column(nullable = false)
	private LocalDate period;

	@Enumerated(EnumType.STRING)
	@Column(name = "time_unit", nullable = false, length = 20)
	private TrendTimeUnit timeUnit;

	@Column(nullable = false, precision = 8, scale = 4)
	private BigDecimal ratio;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected TrendSnapshot() {
	}

	private TrendSnapshot(Keyword keyword, LocalDate period, TrendTimeUnit timeUnit, BigDecimal ratio) {
		this.keyword = keyword;
		this.period = period;
		this.timeUnit = timeUnit;
		this.ratio = ratio;
	}

	public static TrendSnapshot create(Keyword keyword, LocalDate period, TrendTimeUnit timeUnit, BigDecimal ratio) {
		return new TrendSnapshot(keyword, period, timeUnit, ratio);
	}

	public void updateRatio(BigDecimal ratio) {
		this.ratio = ratio;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Keyword getKeyword() {
		return keyword;
	}

	public LocalDate getPeriod() {
		return period;
	}

	public TrendTimeUnit getTimeUnit() {
		return timeUnit;
	}

	public BigDecimal getRatio() {
		return ratio;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
