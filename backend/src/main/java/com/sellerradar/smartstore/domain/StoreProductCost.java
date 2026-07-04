package com.sellerradar.smartstore.domain;

import com.sellerradar.user.domain.User;
import com.sellerradar.wholesale.domain.WholesaleProduct;
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
@Table(name = "store_product_costs")
public class StoreProductCost {
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal DEFAULT_TARGET_MARGIN_RATE = new BigDecimal("25.00");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_product_id", nullable = false)
	private SmartStoreProduct storeProduct;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "wholesale_product_id")
	private WholesaleProduct wholesaleProduct;

	@Column(name = "purchase_cost", nullable = false, precision = 14, scale = 2)
	private BigDecimal purchaseCost;

	@Column(name = "shipping_fee", nullable = false, precision = 14, scale = 2)
	private BigDecimal shippingFee;

	@Column(name = "packaging_fee", nullable = false, precision = 14, scale = 2)
	private BigDecimal packagingFee;

	@Column(name = "extra_cost", nullable = false, precision = 14, scale = 2)
	private BigDecimal extraCost;

	@Column(name = "platform_fee_rate", nullable = false, precision = 6, scale = 2)
	private BigDecimal platformFeeRate;

	@Column(name = "target_margin_rate", nullable = false, precision = 6, scale = 2)
	private BigDecimal targetMarginRate;

	@Lob
	private String memo;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected StoreProductCost() {
	}

	private StoreProductCost(SmartStoreProduct storeProduct) {
		this.storeProduct = storeProduct;
		this.user = storeProduct.getUser();
	}

	public static StoreProductCost create(SmartStoreProduct storeProduct) {
		return new StoreProductCost(storeProduct);
	}

	public void update(
			WholesaleProduct wholesaleProduct,
			BigDecimal purchaseCost,
			BigDecimal shippingFee,
			BigDecimal packagingFee,
			BigDecimal extraCost,
			BigDecimal platformFeeRate,
			BigDecimal targetMarginRate,
			String memo
	) {
		this.wholesaleProduct = wholesaleProduct;
		this.purchaseCost = safe(purchaseCost);
		this.shippingFee = safe(shippingFee);
		this.packagingFee = safe(packagingFee);
		this.extraCost = safe(extraCost);
		this.platformFeeRate = safe(platformFeeRate);
		this.targetMarginRate = targetMarginRate == null ? DEFAULT_TARGET_MARGIN_RATE : safe(targetMarginRate);
		this.memo = normalize(memo);
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

	private static BigDecimal safe(BigDecimal value) {
		if (value == null || value.signum() < 0) {
			return ZERO;
		}
		return value;
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public Long getId() {
		return id;
	}

	public SmartStoreProduct getStoreProduct() {
		return storeProduct;
	}

	public User getUser() {
		return user;
	}

	public WholesaleProduct getWholesaleProduct() {
		return wholesaleProduct;
	}

	public BigDecimal getPurchaseCost() {
		return purchaseCost;
	}

	public BigDecimal getShippingFee() {
		return shippingFee;
	}

	public BigDecimal getPackagingFee() {
		return packagingFee;
	}

	public BigDecimal getExtraCost() {
		return extraCost;
	}

	public BigDecimal getPlatformFeeRate() {
		return platformFeeRate;
	}

	public BigDecimal getTargetMarginRate() {
		return targetMarginRate;
	}

	public String getMemo() {
		return memo;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
