package com.sellerradar.shopping.domain;

import com.sellerradar.keyword.domain.Keyword;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
		name = "shopping_search_snapshots",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_shopping_snapshot_keyword_date_sort",
				columnNames = {"keyword_id", "search_date", "sort_type"}
		)
)
public class ShoppingPriceSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;

	@Column(name = "search_date", nullable = false)
	private LocalDate searchDate;

	@Column(nullable = false, length = 200)
	private String query;

	@Column(name = "sort_type", nullable = false, length = 20)
	private String sortType;

	@Column(name = "display_count", nullable = false)
	private int displayCount;

	@Column(name = "total_count")
	private Integer totalCount;

	@Column(name = "min_price")
	private BigDecimal minPrice;

	@Column(name = "max_price")
	private BigDecimal maxPrice;

	@Column(name = "avg_price")
	private BigDecimal avgPrice;

	@Column(name = "median_price")
	private BigDecimal medianPrice;

	@Column(name = "price_spread_rate", precision = 8, scale = 4)
	private BigDecimal priceSpreadRate;

	@Enumerated(EnumType.STRING)
	@Column(name = "competition_level", nullable = false, length = 30)
	private ShoppingCompetitionLevel competitionLevel;

	@Column(name = "api_cache_hit", nullable = false)
	private boolean apiCacheHit;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ShoppingSnapshotStatus status;

	@Column(name = "error_message")
	private String errorMessage;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_summary")
	private String rawSummary;

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("rankNo ASC")
	private List<ShoppingTopItem> topItems = new ArrayList<>();

	@Column(name = "fetched_at", nullable = false)
	private OffsetDateTime fetchedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected ShoppingPriceSnapshot() {
	}

	private ShoppingPriceSnapshot(
			Keyword keyword,
			LocalDate searchDate,
			String query,
			String sortType,
			int displayCount,
			Integer totalCount,
			Integer minPrice,
			Integer maxPrice,
			Integer avgPrice,
			Integer medianPrice,
			String rawSummary,
			OffsetDateTime fetchedAt,
			boolean apiCacheHit,
			ShoppingSnapshotStatus status,
			String errorMessage
	) {
		this.keyword = keyword;
		this.searchDate = searchDate;
		this.query = query;
		this.sortType = sortType;
		this.displayCount = displayCount;
		this.totalCount = totalCount;
		this.minPrice = amount(minPrice);
		this.maxPrice = amount(maxPrice);
		this.avgPrice = amount(avgPrice);
		this.medianPrice = amount(medianPrice);
		this.priceSpreadRate = priceSpreadRate(this.minPrice, this.maxPrice, this.avgPrice);
		this.competitionLevel = ShoppingCompetitionLevel.UNKNOWN;
		this.apiCacheHit = apiCacheHit;
		this.status = status;
		this.errorMessage = errorMessage;
		this.rawSummary = rawSummary;
		this.fetchedAt = fetchedAt;
	}

	public static ShoppingPriceSnapshot create(
			Keyword keyword,
			LocalDate searchDate,
			long totalResults,
			Integer minPrice,
			Integer maxPrice,
			Integer avgPrice,
			String rawJson
	) {
		return createSuccess(
				keyword,
				searchDate,
				keyword.getKeyword(),
				"sim",
				100,
				toInteger(totalResults),
				minPrice,
				maxPrice,
				avgPrice,
				null,
				rawJson,
				null,
				false
		);
	}

	public static ShoppingPriceSnapshot createSuccess(
			Keyword keyword,
			LocalDate searchDate,
			String query,
			String sortType,
			int displayCount,
			Integer totalCount,
			Integer minPrice,
			Integer maxPrice,
			Integer avgPrice,
			Integer medianPrice,
			String rawSummary,
			OffsetDateTime fetchedAt,
			boolean apiCacheHit
	) {
		return new ShoppingPriceSnapshot(
				keyword,
				searchDate,
				query,
				sortType,
				displayCount,
				totalCount,
				minPrice,
				maxPrice,
				avgPrice,
				medianPrice,
				rawSummary,
				fetchedAt,
				apiCacheHit,
				ShoppingSnapshotStatus.SUCCESS,
				null
		);
	}

	public void addTopItem(ShoppingTopItem topItem) {
		topItem.attachTo(this);
		this.topItems.add(topItem);
		this.competitionLevel = ShoppingCompetitionLevel.from(totalCount, topItems.size());
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (this.fetchedAt == null) {
			this.fetchedAt = now;
		}
		if (this.competitionLevel == null) {
			this.competitionLevel = ShoppingCompetitionLevel.from(totalCount, topItems.size());
		}
		if (this.status == null) {
			this.status = ShoppingSnapshotStatus.SUCCESS;
		}
		this.createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public Keyword getKeyword() {
		return keyword;
	}

	public LocalDate getSearchDate() {
		return searchDate;
	}

	public LocalDate getBaseDate() {
		return searchDate;
	}

	public String getQuery() {
		return query;
	}

	public String getSortType() {
		return sortType;
	}

	public int getDisplayCount() {
		return displayCount;
	}

	public Integer getTotalCount() {
		return totalCount;
	}

	public long getTotalResults() {
		return totalCount == null ? 0L : totalCount.longValue();
	}

	public Integer getMinPrice() {
		return toInteger(minPrice);
	}

	public Integer getMaxPrice() {
		return toInteger(maxPrice);
	}

	public Integer getAvgPrice() {
		return toInteger(avgPrice);
	}

	public Integer getMedianPrice() {
		return toInteger(medianPrice);
	}

	public BigDecimal getPriceSpreadRate() {
		return priceSpreadRate;
	}

	public ShoppingCompetitionLevel getCompetitionLevel() {
		return competitionLevel;
	}

	public boolean isApiCacheHit() {
		return apiCacheHit;
	}

	public ShoppingSnapshotStatus getStatus() {
		return status;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getRawJson() {
		return rawSummary;
	}

	public String getRawSummary() {
		return rawSummary;
	}

	public List<ShoppingTopItem> getTopItems() {
		return Collections.unmodifiableList(topItems);
	}

	public OffsetDateTime getFetchedAt() {
		return fetchedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	private static BigDecimal amount(Integer value) {
		return value == null ? null : BigDecimal.valueOf(value);
	}

	private static BigDecimal priceSpreadRate(BigDecimal minPrice, BigDecimal maxPrice, BigDecimal avgPrice) {
		if (minPrice == null || maxPrice == null || avgPrice == null || avgPrice.compareTo(BigDecimal.ZERO) <= 0) {
			return null;
		}
		return maxPrice.subtract(minPrice).divide(avgPrice, 4, RoundingMode.HALF_UP);
	}

	private static Integer toInteger(BigDecimal value) {
		return value == null ? null : value.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static Integer toInteger(long value) {
		if (value > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (value < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) value;
	}
}
