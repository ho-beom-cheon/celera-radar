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
@Table(name = "wholesale_uploads")
public class WholesaleFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "source_name", length = 100)
	private String sourceName;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "file_path", nullable = false, length = 1000)
	private String storedPath;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "file_type", nullable = false, length = 20)
	private String fileType;

	@Enumerated(EnumType.STRING)
	@Column(name = "requested_encoding", nullable = false, length = 20)
	private CsvEncoding requestedEncoding;

	@Enumerated(EnumType.STRING)
	@Column(name = "detected_encoding", nullable = false, length = 20)
	private CsvEncoding detectedEncoding;

	@Column(name = "total_rows", nullable = false)
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

	@Column(name = "mapping_image_url", length = 255)
	private String mappingImageUrl;

	@Column(name = "mapping_product_url", length = 255)
	private String mappingProductUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WholesaleFileStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "raw_expires_at", nullable = false)
	private OffsetDateTime rawExpiresAt;

	@Column(name = "raw_deleted_at")
	private OffsetDateTime rawDeletedAt;

	@Column(name = "raw_delete_failed_at")
	private OffsetDateTime rawDeleteFailedAt;

	protected WholesaleFile() {
	}

	private WholesaleFile(
			User user,
			String sourceName,
			String originalFilename,
			String storedPath,
			long fileSize,
			String fileType,
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
		this.fileType = fileType;
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
			String fileType,
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
				fileType,
				requestedEncoding,
				detectedEncoding,
				rowCount,
				detectedColumns
		);
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
		return uploaded(
				user,
				sourceName,
				originalFilename,
				storedPath,
				fileSize,
				"CSV",
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
		updateMapping(productName, supplyPrice, shippingFee, category, null, productUrl);
	}

	public void updateMapping(
			String productName,
			String supplyPrice,
			String shippingFee,
			String category,
			String imageUrl,
			String productUrl
	) {
		this.mappingProductName = productName;
		this.mappingSupplyPrice = supplyPrice;
		this.mappingShippingFee = shippingFee;
		this.mappingCategory = category;
		this.mappingImageUrl = imageUrl;
		this.mappingProductUrl = productUrl;
		this.status = WholesaleFileStatus.MAPPED;
	}

	public void markParsed() {
		this.status = WholesaleFileStatus.PARSED;
	}

	public void markFailed() {
		this.status = WholesaleFileStatus.FAILED;
	}

	public void configureRawRetention(OffsetDateTime expiresAt) {
		this.rawExpiresAt = expiresAt;
	}

	public void markRawDeleted(OffsetDateTime deletedAt) {
		this.rawDeletedAt = deletedAt;
		this.rawDeleteFailedAt = null;
	}

	public void markRawDeleteFailed(OffsetDateTime failedAt) {
		this.rawDeleteFailedAt = failedAt;
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
		if (rawExpiresAt == null) {
			this.rawExpiresAt = now.plusDays(7);
		}
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

	public String getFileType() {
		return fileType;
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

	public String getMappingImageUrl() {
		return mappingImageUrl;
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

	public OffsetDateTime getRawExpiresAt() {
		return rawExpiresAt;
	}

	public OffsetDateTime getRawDeletedAt() {
		return rawDeletedAt;
	}

	public OffsetDateTime getRawDeleteFailedAt() {
		return rawDeleteFailedAt;
	}
}
