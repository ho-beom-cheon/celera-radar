package com.sellerradar.shopping.domain;

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
import java.time.OffsetDateTime;

@Entity
@Table(name = "shopping_top_item")
public class ShoppingTopItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", nullable = false)
	private ShoppingPriceSnapshot snapshot;

	@Column(name = "item_rank", nullable = false)
	private int itemRank;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(length = 1000)
	private String link;

	@Column(length = 1000)
	private String image;

	@Column
	private Integer lprice;

	@Column
	private Integer hprice;

	@Column(name = "mall_name", length = 255)
	private String mallName;

	@Column(name = "product_id", length = 100)
	private String productId;

	@Column(name = "product_type", length = 20)
	private String productType;

	@Column(length = 255)
	private String brand;

	@Column(length = 255)
	private String maker;

	@Column(length = 100)
	private String category1;

	@Column(length = 100)
	private String category2;

	@Column(length = 100)
	private String category3;

	@Column(length = 100)
	private String category4;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected ShoppingTopItem() {
	}

	private ShoppingTopItem(
			int itemRank,
			String title,
			String link,
			String image,
			Integer lprice,
			Integer hprice,
			String mallName,
			String productId,
			String productType,
			String brand,
			String maker,
			String category1,
			String category2,
			String category3,
			String category4
	) {
		this.itemRank = itemRank;
		this.title = title;
		this.link = link;
		this.image = image;
		this.lprice = lprice;
		this.hprice = hprice;
		this.mallName = mallName;
		this.productId = productId;
		this.productType = productType;
		this.brand = brand;
		this.maker = maker;
		this.category1 = category1;
		this.category2 = category2;
		this.category3 = category3;
		this.category4 = category4;
	}

	public static ShoppingTopItem create(
			int itemRank,
			String title,
			String link,
			String image,
			Integer lprice,
			Integer hprice,
			String mallName,
			String productId,
			String productType,
			String brand,
			String maker,
			String category1,
			String category2,
			String category3,
			String category4
	) {
		return new ShoppingTopItem(
				itemRank,
				title,
				link,
				image,
				lprice,
				hprice,
				mallName,
				productId,
				productType,
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
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public ShoppingPriceSnapshot getSnapshot() {
		return snapshot;
	}

	public int getItemRank() {
		return itemRank;
	}

	public String getTitle() {
		return title;
	}

	public String getLink() {
		return link;
	}

	public String getImage() {
		return image;
	}

	public Integer getLprice() {
		return lprice;
	}

	public Integer getHprice() {
		return hprice;
	}

	public String getMallName() {
		return mallName;
	}

	public String getProductId() {
		return productId;
	}

	public String getProductType() {
		return productType;
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

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
