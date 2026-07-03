package com.sellerradar.shopping.domain;

import com.sellerradar.keyword.domain.Keyword;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
		name = "shopping_price_snapshot",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_shopping_price_snapshot_keyword_base_date",
				columnNames = {"keyword_id", "base_date"}
		)
)
public class ShoppingPriceSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;

	@Column(name = "base_date", nullable = false)
	private LocalDate baseDate;

	@Column(name = "total_results", nullable = false)
	private long totalResults;

	@Column(name = "min_price")
	private Integer minPrice;

	@Column(name = "max_price")
	private Integer maxPrice;

	@Column(name = "avg_price")
	private Integer avgPrice;

	@Lob
	@Column(name = "raw_json", nullable = false)
	private String rawJson;

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("itemRank ASC")
	private List<ShoppingTopItem> topItems = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected ShoppingPriceSnapshot() {
	}

	private ShoppingPriceSnapshot(
			Keyword keyword,
			LocalDate baseDate,
			long totalResults,
			Integer minPrice,
			Integer maxPrice,
			Integer avgPrice,
			String rawJson
	) {
		this.keyword = keyword;
		this.baseDate = baseDate;
		this.totalResults = totalResults;
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
		this.avgPrice = avgPrice;
		this.rawJson = rawJson;
	}

	public static ShoppingPriceSnapshot create(
			Keyword keyword,
			LocalDate baseDate,
			long totalResults,
			Integer minPrice,
			Integer maxPrice,
			Integer avgPrice,
			String rawJson
	) {
		return new ShoppingPriceSnapshot(keyword, baseDate, totalResults, minPrice, maxPrice, avgPrice, rawJson);
	}

	public void addTopItem(ShoppingTopItem topItem) {
		topItem.attachTo(this);
		this.topItems.add(topItem);
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

	public LocalDate getBaseDate() {
		return baseDate;
	}

	public long getTotalResults() {
		return totalResults;
	}

	public Integer getMinPrice() {
		return minPrice;
	}

	public Integer getMaxPrice() {
		return maxPrice;
	}

	public Integer getAvgPrice() {
		return avgPrice;
	}

	public String getRawJson() {
		return rawJson;
	}

	public List<ShoppingTopItem> getTopItems() {
		return Collections.unmodifiableList(topItems);
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
