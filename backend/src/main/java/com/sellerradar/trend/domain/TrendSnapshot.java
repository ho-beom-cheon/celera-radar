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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
		name = "trend_snapshots",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_trend_snapshot_keyword_date_period_time_unit_filter",
				columnNames = {"keyword_id", "snapshot_date", "data_period", "time_unit", "device", "gender", "ages"}
		)
)
public class TrendSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(name = "period_end", nullable = false)
	private LocalDate periodEnd;

	@Enumerated(EnumType.STRING)
	@Column(name = "time_unit", nullable = false, length = 20)
	private TrendTimeUnit timeUnit;

	@Column(name = "data_period", nullable = false)
	private LocalDate dataPeriod;

	@Column(nullable = false, precision = 10, scale = 4)
	private BigDecimal ratio;

	@Column(nullable = false, length = 20)
	private String device;

	@Column(nullable = false, length = 20)
	private String gender;

	@Column(nullable = false, length = 100)
	private String ages;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_payload")
	private String rawPayload;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected TrendSnapshot() {
	}

	private TrendSnapshot(Keyword keyword, LocalDate period, TrendTimeUnit timeUnit, BigDecimal ratio) {
		this(keyword, period, period, period, period, timeUnit, ratio, "ALL", "ALL", "", null);
	}

	private TrendSnapshot(
			Keyword keyword,
			LocalDate snapshotDate,
			LocalDate periodStart,
			LocalDate periodEnd,
			LocalDate dataPeriod,
			TrendTimeUnit timeUnit,
			BigDecimal ratio,
			String device,
			String gender,
			String ages,
			String rawPayload
	) {
		this.keyword = keyword;
		this.snapshotDate = snapshotDate;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.dataPeriod = dataPeriod;
		this.timeUnit = timeUnit;
		this.ratio = ratio;
		this.device = normalizeFilter(device, "ALL");
		this.gender = normalizeFilter(gender, "ALL");
		this.ages = normalizeFilter(ages, "");
		this.rawPayload = rawPayload;
	}

	public static TrendSnapshot create(Keyword keyword, LocalDate period, TrendTimeUnit timeUnit, BigDecimal ratio) {
		return new TrendSnapshot(keyword, period, timeUnit, ratio);
	}

	public static TrendSnapshot create(
			Keyword keyword,
			LocalDate snapshotDate,
			LocalDate periodStart,
			LocalDate periodEnd,
			LocalDate dataPeriod,
			TrendTimeUnit timeUnit,
			BigDecimal ratio,
			String rawPayload
	) {
		return new TrendSnapshot(
				keyword,
				snapshotDate,
				periodStart,
				periodEnd,
				dataPeriod,
				timeUnit,
				ratio,
				"ALL",
				"ALL",
				"",
				rawPayload
		);
	}

	public void updateRatio(BigDecimal ratio) {
		this.ratio = ratio;
	}

	public void updateRatio(BigDecimal ratio, String rawPayload) {
		this.ratio = ratio;
		this.rawPayload = rawPayload;
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

	public LocalDate getSnapshotDate() {
		return snapshotDate;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public LocalDate getPeriod() {
		return dataPeriod;
	}

	public LocalDate getDataPeriod() {
		return dataPeriod;
	}

	public TrendTimeUnit getTimeUnit() {
		return timeUnit;
	}

	public BigDecimal getRatio() {
		return ratio;
	}

	public String getDevice() {
		return device;
	}

	public String getGender() {
		return gender;
	}

	public String getAges() {
		return ages;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	private static String normalizeFilter(String value, String defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value;
	}
}
