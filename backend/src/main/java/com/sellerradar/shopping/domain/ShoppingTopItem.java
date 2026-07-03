package com.sellerradar.shopping.domain;

import com.sellerradar.keyword.domain.Keyword;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
		name = "shopping_item_snapshots",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_shopping_item_snapshot_rank",
				columnNames = {"snapshot_id", "rank_no"}
		)
)
public class ShoppingTopItem {
	private static final String SOURCE_NAVER_SHOPPING = "NAVER_SHOPPING";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", nullable = false)
	private ShoppingPriceSnapshot snapshot;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "keyword_id", nullable = false)
	private Keyword keyword;

	@Column(nullable = false, length = 30)
	private String source;

	@Column(name = "source_product_id", length = 100)
	private String sourceProductId;

	@Column(name = "rank_no", nullable = false)
	private int rankNo;

	@Column(name = "title_raw", nullable = false)
	private String titleRaw;

	@Column(name = "title_clean")
	private String titleClean;

	@Column(name = "product_url", nullable = false)
	private String productUrl;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "low_price")
	private BigDecimal lowPrice;

	@Column(name = "high_price")
	private BigDecimal highPrice;

	@Column(name = "mall_name", length = 200)
	private String mallName;

	@Column(length = 200)
	private String brand;

	@Column(length = 200)
	private String maker;

	@Column(length = 100)
	private String category1;

	@Column(length = 100)
	private String category2;

	@Column(length = 100)
	private String category3;

	@Column(length = 100)
	private String category4;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_item")
	private String rawItem;

	@Column(name = "fetched_at", nullable = false)
	private OffsetDateTime fetchedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected ShoppingTopItem() {
	}

	private ShoppingTopItem(
			int rankNo,
			String title,
			String productUrl,
			String imageUrl,
			Integer lowPrice,
			Integer highPrice,
			String mallName,
			String sourceProductId,
			String brand,
			String maker,
			String category1,
			String category2,
			String category3,
			String category4
	) {
		this.source = SOURCE_NAVER_SHOPPING;
		this.sourceProductId = sourceProductId;
		this.rankNo = rankNo;
		this.titleRaw = title;
		this.titleClean = cleanTitle(title);
		this.productUrl = productUrl;
		this.imageUrl = imageUrl;
		this.lowPrice = amount(lowPrice);
		this.highPrice = amount(highPrice);
		this.mallName = mallName;
		this.brand = brand;
		this.maker = maker;
		this.category1 = category1;
		this.category2 = category2;
		this.category3 = category3;
		this.category4 = category4;
	}

	public static ShoppingTopItem create(
			int rankNo,
			String title,
			String productUrl,
			String imageUrl,
			Integer lowPrice,
			Integer highPrice,
			String mallName,
			String sourceProductId,
			String productType,
			String brand,
			String maker,
			String category1,
			String category2,
			String category3,
			String category4
	) {
		return new ShoppingTopItem(
				rankNo,
				title,
				productUrl,
				imageUrl,
				lowPrice,
				highPrice,
				mallName,
				sourceProductId,
				brand,
				maker,
				category1,
				category2,
				category3,
				category4
		);
	}

	void attachTo(ShoppingPriceSnapshot snapshot) {
		this.snapshot = snapshot;
		this.keyword = snapshot.getKeyword();
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (this.fetchedAt == null) {
			this.fetchedAt = now;
		}
		this.createdAt = now;
	}

	public Long getId() {
		return id;
	}

	public ShoppingPriceSnapshot getSnapshot() {
		return snapshot;
	}

	public Keyword getKeyword() {
		return keyword;
	}

	public String getSource() {
		return source;
	}

	public String getSourceProductId() {
		return sourceProductId;
	}

	public int getItemRank() {
		return rankNo;
	}

	public int getRankNo() {
		return rankNo;
	}

	public String getTitle() {
		return titleRaw;
	}

	public String getTitleRaw() {
		return titleRaw;
	}

	public String getTitleClean() {
		return titleClean;
	}

	public String getLink() {
		return productUrl;
	}

	public String getProductUrl() {
		return productUrl;
	}

	public String getImage() {
		return imageUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public Integer getLprice() {
		return toInteger(lowPrice);
	}

	public Integer getLowPrice() {
		return toInteger(lowPrice);
	}

	public Integer getHprice() {
		return toInteger(highPrice);
	}

	public Integer getHighPrice() {
		return toInteger(highPrice);
	}

	public String getMallName() {
		return mallName;
	}

	public String getProductId() {
		return sourceProductId;
	}

	public String getProductType() {
		return null;
	}

	public String getBrand() {
		return brand;
	}

	public String getMaker() {
		return maker;
	}

	public String getCategory1() {
		return category1;
	}

	public String getCategory2() {
		return category2;
	}

	public String getCategory3() {
		return category3;
	}

	public String getCategory4() {
		return category4;
	}

	public String getRawItem() {
		return rawItem;
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

	private static Integer toInteger(BigDecimal value) {
		return value == null ? null : value.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static String cleanTitle(String value) {
		if (value == null) {
			return null;
		}
		return value.replaceAll("<[^>]*>", "").trim();
	}
}
