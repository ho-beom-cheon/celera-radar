package com.sellerradar.wholesale.domain;

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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "wholesale_products")
public class WholesaleProduct {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "upload_id", nullable = false)
	private WholesaleFile file;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "row_no", nullable = false)
	private int rowNo;

	@Column(name = "source_name", length = 100)
	private String sourceName;

	@Column(name = "external_product_id", length = 200)
	private String externalProductId;

	@Column(name = "product_name", columnDefinition = "TEXT")
	private String productName;

	@Column(name = "normalized_product_name", columnDefinition = "TEXT")
	private String normalizedName;

	@Column(name = "supply_price", columnDefinition = "NUMERIC(14,2)")
	private Integer supplyPrice;

	@Column(name = "shipping_fee", columnDefinition = "NUMERIC(14,2)")
	private Integer shippingFee;

	@Column(name = "category", length = 200)
	private String sourceCategory;

	@Column(name = "image_url", columnDefinition = "TEXT")
	private String imageUrl;

	@Column(name = "product_url", columnDefinition = "TEXT")
	private String productUrl;

	@Column(length = 200)
	private String brand;

	@Column(length = 200)
	private String maker;

	@Column(name = "option_name", length = 300)
	private String optionName;

	@Column(name = "stock_status", length = 100)
	private String stockStatus;

	@Column(name = "is_sold_out", nullable = false)
	private boolean soldOut;

	@Column(columnDefinition = "TEXT")
	private String memo;

	@Enumerated(EnumType.STRING)
	@Column(name = "parse_status", nullable = false, length = 20)
	private WholesaleProductParseStatus parseStatus;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_row")
	private String rawRow;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected WholesaleProduct() {
	}

	private WholesaleProduct(
			WholesaleFile file,
			int rowNo,
			String productName,
			String normalizedName,
			Integer supplyPrice,
			Integer shippingFee,
			String sourceCategory,
			String imageUrl,
			String productUrl,
			WholesaleProductParseStatus parseStatus,
			String errorMessage
	) {
		this.file = file;
		this.user = file.getUser();
		this.rowNo = rowNo;
		this.sourceName = file.getSourceName();
		this.productName = productName;
		this.normalizedName = normalizedName;
		this.supplyPrice = supplyPrice;
		this.shippingFee = shippingFee;
		this.sourceCategory = sourceCategory;
		this.imageUrl = imageUrl;
		this.productUrl = productUrl;
		this.parseStatus = parseStatus;
		this.errorMessage = errorMessage;
	}

	public static WholesaleProduct parsed(
			WholesaleFile file,
			int rowNo,
			String productName,
			String normalizedName,
			Integer supplyPrice,
			Integer shippingFee,
			String sourceCategory,
			String imageUrl,
			String productUrl
	) {
		return new WholesaleProduct(
				file,
				rowNo,
				productName,
				normalizedName,
				supplyPrice,
				shippingFee,
				sourceCategory,
				imageUrl,
				productUrl,
				WholesaleProductParseStatus.PARSED,
				null
		);
	}

	public static WholesaleProduct invalid(WholesaleFile file, int rowNo, String errorMessage) {
		return new WholesaleProduct(
				file,
				rowNo,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				WholesaleProductParseStatus.INVALID,
				errorMessage
		);
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public WholesaleFile getFile() {
		return file;
	}

	public int getRowNo() {
		return rowNo;
	}

	public String getProductName() {
		return productName;
	}

	public User getUser() {
		return user;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getExternalProductId() {
		return externalProductId;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public Integer getSupplyPrice() {
		return supplyPrice;
	}

	public Integer getShippingFee() {
		return shippingFee;
	}

	public String getSourceCategory() {
		return sourceCategory;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getProductUrl() {
		return productUrl;
	}

	public String getBrand() {
		return brand;
	}

	public String getMaker() {
		return maker;
	}

	public String getOptionName() {
		return optionName;
	}

	public String getStockStatus() {
		return stockStatus;
	}

	public boolean isSoldOut() {
		return soldOut;
	}

	public String getMemo() {
		return memo;
	}

	public WholesaleProductParseStatus getParseStatus() {
		return parseStatus;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public String getRawRow() {
		return rawRow;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
