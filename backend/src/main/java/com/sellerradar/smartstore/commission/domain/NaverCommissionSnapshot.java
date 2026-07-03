package com.sellerradar.smartstore.commission.domain;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "naver_commission_snapshots")
public class NaverCommissionSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "connection_id", nullable = false)
	private SmartStoreConnection connection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "base_date", nullable = false)
	private LocalDate baseDate;

	@Column(name = "product_order_no", nullable = false, length = 100)
	private String productOrderNo;

	@Column(name = "commission_type", nullable = false, length = 100)
	private String commissionType;

	@Column(name = "commission_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal commissionAmount;

	@Lob
	@Column(name = "raw_payload", nullable = false)
	private String rawPayload;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected NaverCommissionSnapshot() {
	}

	private NaverCommissionSnapshot(
			SmartStoreConnection connection,
			LocalDate baseDate,
			String productOrderNo,
			String commissionType,
			BigDecimal commissionAmount,
			String rawPayload
	) {
		this.connection = connection;
		this.user = connection.getUser();
		this.baseDate = baseDate;
		this.productOrderNo = normalizeRequired(productOrderNo, "productOrderNo");
		this.commissionType = normalizeRequired(commissionType, "commissionType");
		this.commissionAmount = safeAmount(commissionAmount);
		this.rawPayload = normalizePayload(rawPayload);
	}

	public static NaverCommissionSnapshot create(
			SmartStoreConnection connection,
			LocalDate baseDate,
			String productOrderNo,
			String commissionType,
			BigDecimal commissionAmount,
			String rawPayload
	) {
		return new NaverCommissionSnapshot(
				connection,
				baseDate,
				productOrderNo,
				commissionType,
				commissionAmount,
				rawPayload
		);
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	private static BigDecimal safeAmount(BigDecimal value) {
		return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
	}

	private static String normalizeRequired(String value, String field) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(field + " is required.");
		}
		return normalized;
	}

	private static String normalizePayload(String value) {
		String normalized = normalize(value);
		return normalized == null ? "{}" : normalized;
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

	public SmartStoreConnection getConnection() {
		return connection;
	}

	public User getUser() {
		return user;
	}

	public LocalDate getBaseDate() {
		return baseDate;
	}

	public String getProductOrderNo() {
		return productOrderNo;
	}

	public String getCommissionType() {
		return commissionType;
	}

	public BigDecimal getCommissionAmount() {
		return commissionAmount;
	}

	public String getRawPayload() {
		return rawPayload;
	}
}
