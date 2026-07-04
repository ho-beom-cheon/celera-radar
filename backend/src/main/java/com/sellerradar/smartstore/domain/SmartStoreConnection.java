package com.sellerradar.smartstore.domain;

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

@Entity
@Table(name = "naver_store_connections")
public class SmartStoreConnection {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "store_name", length = 200)
	private String storeName;

	@Column(name = "store_id", length = 100)
	private String storeId;

	@Column(name = "seller_id", length = 100)
	private String sellerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "connection_status", nullable = false, length = 30)
	private SmartStoreConnectionStatus connectionStatus;

	@Column(name = "access_token_encrypted", columnDefinition = "TEXT")
	private String accessTokenEncrypted;

	@Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
	private String refreshTokenEncrypted;

	@Column(name = "token_expires_at")
	private OffsetDateTime tokenExpiresAt;

	@Column(name = "last_synced_at")
	private OffsetDateTime lastSyncedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected SmartStoreConnection() {
	}

	private SmartStoreConnection(User user, String storeName, String storeId, String sellerId) {
		this.user = user;
		this.storeName = normalize(storeName);
		this.storeId = normalize(storeId);
		this.sellerId = normalize(sellerId);
		this.connectionStatus = SmartStoreConnectionStatus.DISCONNECTED;
	}

	public static SmartStoreConnection disconnected(
			User user,
			String storeName,
			String storeId,
			String sellerId
	) {
		return new SmartStoreConnection(user, storeName, storeId, sellerId);
	}

	public void markConnected(
			String accessTokenEncrypted,
			String refreshTokenEncrypted,
			OffsetDateTime tokenExpiresAt,
			OffsetDateTime syncedAt
	) {
		this.accessTokenEncrypted = requireEncryptedToken(accessTokenEncrypted, "accessTokenEncrypted");
		this.refreshTokenEncrypted = requireEncryptedToken(refreshTokenEncrypted, "refreshTokenEncrypted");
		this.tokenExpiresAt = tokenExpiresAt;
		this.lastSyncedAt = syncedAt;
		this.connectionStatus = SmartStoreConnectionStatus.CONNECTED;
	}

	public void markExpired(OffsetDateTime checkedAt) {
		this.connectionStatus = SmartStoreConnectionStatus.EXPIRED;
		this.lastSyncedAt = checkedAt;
	}

	public void markError(OffsetDateTime checkedAt) {
		this.connectionStatus = SmartStoreConnectionStatus.ERROR;
		this.lastSyncedAt = checkedAt;
	}

	public void markSynced(OffsetDateTime syncedAt) {
		this.lastSyncedAt = syncedAt;
	}

	public void disconnect() {
		this.connectionStatus = SmartStoreConnectionStatus.DISCONNECTED;
		this.accessTokenEncrypted = null;
		this.refreshTokenEncrypted = null;
		this.tokenExpiresAt = null;
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

	private static String requireEncryptedToken(String value, String field) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalArgumentException(field + " must contain encrypted token text.");
		}
		return normalized;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getStoreName() {
		return storeName;
	}

	public String getStoreId() {
		return storeId;
	}

	public String getSellerId() {
		return sellerId;
	}

	public SmartStoreConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	public String getAccessTokenEncrypted() {
		return accessTokenEncrypted;
	}

	public String getRefreshTokenEncrypted() {
		return refreshTokenEncrypted;
	}

	public OffsetDateTime getTokenExpiresAt() {
		return tokenExpiresAt;
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
