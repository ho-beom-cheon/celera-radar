package com.sellerradar.smartstore.order.domain;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.user.domain.User;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "naver_order_snapshots")
public class NaverOrderSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "connection_id", nullable = false)
	private SmartStoreConnection connection;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "order_no", nullable = false, length = 100)
	private String orderNo;

	@Column(name = "product_order_no", nullable = false, length = 100)
	private String productOrderNo;

	@Column(name = "order_date", nullable = false)
	private OffsetDateTime orderDate;

	@Column(name = "payment_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal paymentAmount;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "order_status", nullable = false, length = 50)
	private String orderStatus;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_payload", nullable = false)
	private String rawPayload;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected NaverOrderSnapshot() {
	}

	private NaverOrderSnapshot(
			SmartStoreConnection connection,
			String orderNo,
			String productOrderNo,
			OffsetDateTime orderDate,
			BigDecimal paymentAmount,
			int quantity,
			String orderStatus,
			String rawPayload
	) {
		this.connection = connection;
		this.user = connection.getUser();
		this.orderNo = normalizeRequired(orderNo, "orderNo");
		this.productOrderNo = normalizeRequired(productOrderNo, "productOrderNo");
		this.orderDate = orderDate;
		this.paymentAmount = safeAmount(paymentAmount);
		this.quantity = Math.max(quantity, 1);
		this.orderStatus = normalizeRequired(orderStatus, "orderStatus");
		this.rawPayload = normalizePayload(rawPayload);
	}

	public static NaverOrderSnapshot create(
			SmartStoreConnection connection,
			String orderNo,
			String productOrderNo,
			OffsetDateTime orderDate,
			BigDecimal paymentAmount,
			int quantity,
			String orderStatus,
			String rawPayload
	) {
		return new NaverOrderSnapshot(
				connection,
				orderNo,
				productOrderNo,
				orderDate,
				paymentAmount,
				quantity,
				orderStatus,
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

	public String getOrderNo() {
		return orderNo;
	}

	public String getProductOrderNo() {
		return productOrderNo;
	}

	public OffsetDateTime getOrderDate() {
		return orderDate;
	}

	public BigDecimal getPaymentAmount() {
		return paymentAmount;
	}

	public int getQuantity() {
		return quantity;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public String getRawPayload() {
		return rawPayload;
	}
}
