package com.sellerradar.smartstore.settlement.domain;

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
@Table(name = "naver_settlement_snapshots")
public class NaverSettlementSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "connection_id", nullable = false)
	private SmartStoreConnection connection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "settlement_date", nullable = false)
	private LocalDate settlementDate;

	@Column(name = "product_order_no", nullable = false, length = 100)
	private String productOrderNo;

	@Column(name = "settlement_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal settlementAmount;

	@Lob
	@Column(name = "raw_payload", nullable = false)
	private String rawPayload;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected NaverSettlementSnapshot() {
	}

	private NaverSettlementSnapshot(
			SmartStoreConnection connection,
			LocalDate settlementDate,
			String productOrderNo,
			BigDecimal settlementAmount,
			String rawPayload
	) {
		this.connection = connection;
		this.user = connection.getUser();
		this.settlementDate = settlementDate;
		this.productOrderNo = normalizeRequired(productOrderNo, "productOrderNo");
		this.settlementAmount = safeAmount(settlementAmount);
		this.rawPayload = normalizePayload(rawPayload);
	}

	public static NaverSettlementSnapshot create(
			SmartStoreConnection connection,
			LocalDate settlementDate,
			String productOrderNo,
			BigDecimal settlementAmount,
			String rawPayload
	) {
		return new NaverSettlementSnapshot(connection, settlementDate, productOrderNo, settlementAmount, rawPayload);
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

	public LocalDate getSettlementDate() {
		return settlementDate;
	}

	public String getProductOrderNo() {
		return productOrderNo;
	}

	public BigDecimal getSettlementAmount() {
		return settlementAmount;
	}

	public String getRawPayload() {
		return rawPayload;
	}
}
