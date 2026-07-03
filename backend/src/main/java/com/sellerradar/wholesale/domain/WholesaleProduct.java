package com.sellerradar.wholesale.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wholesale_product")
public class WholesaleProduct {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "file_id", nullable = false)
	private WholesaleFile file;

	@Column(name = "row_no", nullable = false)
	private int rowNo;

	@Column(name = "product_name", length = 255)
	private String productName;

	@Column(name = "normalized_name", length = 255)
	private String normalizedName;

	@Column(name = "supply_price")
	private Integer supplyPrice;

	@Column(name = "shipping_fee")
	private Integer shippingFee;

	@Column(name = "source_category", length = 255)
	private String sourceCategory;

	@Lob
	@Column(name = "product_url")
	private String productUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "parse_status", nullable = false, length = 20)
	private WholesaleProductParseStatus parseStatus;

	@Lob
	@Column(name = "error_message")
	private String errorMessage;

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
			String productUrl,
			WholesaleProductParseStatus parseStatus,
			String errorMessage
	) {
		this.file = file;
		this.rowNo = rowNo;
		this.productName = productName;
		this.normalizedName = normalizedName;
		this.supplyPrice = supplyPrice;
		this.shippingFee = shippingFee;
		this.sourceCategory = sourceCategory;
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

	public String getProductUrl() {
		return productUrl;
	}

	public WholesaleProductParseStatus getParseStatus() {
		return parseStatus;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
