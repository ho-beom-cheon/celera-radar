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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "wholesale_file")
public class WholesaleFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "source_name", length = 100)
	private String sourceName;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "stored_path", nullable = false, length = 1000)
	private String storedPath;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "requested_encoding", nullable = false, length = 20)
	private CsvEncoding requestedEncoding;

	@Enumerated(EnumType.STRING)
	@Column(name = "detected_encoding", nullable = false, length = 20)
	private CsvEncoding detectedEncoding;

	@Column(name = "row_count", nullable = false)
	private int rowCount;

	@Column(name = "detected_columns", nullable = false, length = 1000)
	private String detectedColumns;

	@Column(name = "mapping_product_name", length = 255)
	private String mappingProductName;

	@Column(name = "mapping_supply_price", length = 255)
	private String mappingSupplyPrice;

	@Column(name = "mapping_shipping_fee", length = 255)
	private String mappingShippingFee;

	@Column(name = "mapping_category", length = 255)
	private String mappingCategory;

	@Column(name = "mapping_product_url", length = 255)
	private String mappingProductUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WholesaleFileStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected WholesaleFile() {
	}

	private WholesaleFile(
			User user,
			String sourceName,
			String originalFilename,
			String storedPath,
			long fileSize,
			CsvEncoding requestedEncoding,
			CsvEncoding detectedEncoding,
			int rowCount,
			List<String> detectedColumns
	) {
		this.user = user;
		this.sourceName = sourceName;
		this.originalFilename = originalFilename;
		this.storedPath = storedPath;
		this.fileSize = fileSize;
		this.requestedEncoding = requestedEncoding;
		this.detectedEncoding = detectedEncoding;
		this.rowCount = rowCount;
		this.detectedColumns = joinColumns(detectedColumns);
		this.status = WholesaleFileStatus.UPLOADED;
	}

	public static WholesaleFile uploaded(
			User user,
			String sourceName,
			String originalFilename,
			String storedPath,
			long fileSize,
			CsvEncoding requestedEncoding,
			CsvEncoding detectedEncoding,
			int rowCount,
			List<String> detectedColumns
	) {
		return new WholesaleFile(
				user,
				sourceName,
				originalFilename,
				storedPath,
				fileSize,
				requestedEncoding,
				detectedEncoding,
				rowCount,
				detectedColumns
		);
	}

	public void updateMapping(
			String productName,
			String supplyPrice,
			String shippingFee,
			String category,
			String productUrl
	) {
		this.mappingProductName = productName;
		this.mappingSupplyPrice = supplyPrice;
		this.mappingShippingFee = shippingFee;
		this.mappingCategory = category;
		this.mappingProductUrl = productUrl;
		this.status = WholesaleFileStatus.MAPPED;
	}

	public void markParsed() {
		this.status = WholesaleFileStatus.PARSED;
	}

	public void markFailed() {
		this.status = WholesaleFileStatus.FAILED;
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

	public List<String> detectedColumns() {
		if (detectedColumns == null || detectedColumns.isBlank()) {
			return List.of();
		}
		return detectedColumns.lines()
				.map(String::strip)
				.filter(column -> !column.isBlank())
				.toList();
	}

	private static String joinColumns(List<String> columns) {
		if (columns == null || columns.isEmpty()) {
			return "";
		}
		return String.join("\n", columns);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getStoredPath() {
		return storedPath;
	}

	public long getFileSize() {
		return fileSize;
	}

	public CsvEncoding getRequestedEncoding() {
		return requestedEncoding;
	}

	public CsvEncoding getDetectedEncoding() {
		return detectedEncoding;
	}

	public int getRowCount() {
		return rowCount;
	}

	public String getMappingProductName() {
		return mappingProductName;
	}

	public String getMappingSupplyPrice() {
		return mappingSupplyPrice;
	}

	public String getMappingShippingFee() {
		return mappingShippingFee;
	}

	public String getMappingCategory() {
		return mappingCategory;
	}

	public String getMappingProductUrl() {
		return mappingProductUrl;
	}

	public WholesaleFileStatus getStatus() {
		return status;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
