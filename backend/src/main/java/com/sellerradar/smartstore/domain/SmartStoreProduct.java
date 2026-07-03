package com.sellerradar.smartstore.domain;

import com.sellerradar.smartstore.client.SmartStoreProductSyncItem;
import com.sellerradar.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "naver_store_products")
public class SmartStoreProduct {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "connection_id", nullable = false)
	private SmartStoreConnection connection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "channel_product_no", nullable = false, length = 100)
	private String sourceProductId;

	@Column(name = "origin_product_no", length = 100)
	private String originProductNo;

	@Lob
	@Column(name = "product_name", nullable = false)
	private String productName;

	@Column(name = "sale_price", nullable = false, precision = 14, scale = 2)
	private BigDecimal salePrice;

	@Column(name = "sale_status", nullable = false, length = 50)
	private String saleStatus;

	@Lob
	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "category_name", length = 300)
	private String categoryName;

	@Lob
	@Column(name = "raw_payload")
	private String rawPayload;

	@Column(name = "last_synced_at", nullable = false)
	private OffsetDateTime lastSyncedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected SmartStoreProduct() {
	}

	private SmartStoreProduct(
			SmartStoreConnection connection,
			SmartStoreProductSyncItem item,
			OffsetDateTime syncedAt
	) {
		this.connection = connection;
		this.user = connection.getUser();
		this.sourceProductId = normalizeRequired(item.sourceProductId(), "sourceProductId");
		sync(item, syncedAt);
	}

	public static SmartStoreProduct create(
			SmartStoreConnection connection,
			SmartStoreProductSyncItem item,
			OffsetDateTime syncedAt
	) {
		return new SmartStoreProduct(connection, item, syncedAt);
	}

	public void sync(SmartStoreProductSyncItem item, OffsetDateTime syncedAt) {
		this.originProductNo = normalize(item.originProductNo());
		this.productName = normalizeRequired(item.productName(), "productName");
		this.salePrice = item.salePrice();
		this.saleStatus = normalizeRequired(item.saleStatus(), "saleStatus");
		this.imageUrl = normalize(item.imageUrl());
		this.categoryName = normalize(item.categoryName());
		this.rawPayload = normalizeJsonPayload(item.rawPayload());
		this.lastSyncedAt = syncedAt;
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

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String normalizeRequired(String value, String field) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(field + " is required.");
		}
		return normalized;
	}

	private static String normalizeJsonPayload(String value) {
		String normalized = normalize(value);
		return normalized == null ? "{}" : normalized;
	}

	public Long getId() {
		return id;
	}

	public SmartStoreConnection getConnection() {
		return connection;
	}

	public User getUser() {
		return user;
	}

	public String getSourceProductId() {
		return sourceProductId;
	}

	public String getOriginProductNo() {
		return originProductNo;
	}

	public String getProductName() {
		return productName;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public String getSaleStatus() {
		return saleStatus;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public OffsetDateTime getLastSyncedAt() {
		return lastSyncedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
