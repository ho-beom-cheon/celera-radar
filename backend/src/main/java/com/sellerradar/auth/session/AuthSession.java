package com.sellerradar.auth.session;

import com.sellerradar.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	@Column(name = "family_id", nullable = false, updatable = false)
	private UUID familyId;

	@Column(name = "token_hash", nullable = false, updatable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false, updatable = false)
	private OffsetDateTime expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "last_used_at")
	private OffsetDateTime lastUsedAt;

	@Column(name = "rotated_at")
	private OffsetDateTime rotatedAt;

	@Column(name = "revoked_at")
	private OffsetDateTime revokedAt;

	@Column(name = "reuse_detected_at")
	private OffsetDateTime reuseDetectedAt;

	@Column(name = "replaced_by_session_id")
	private Long replacedBySessionId;

	protected AuthSession() {
	}

	private AuthSession(
			User user,
			UUID familyId,
			String tokenHash,
			OffsetDateTime expiresAt,
			OffsetDateTime createdAt
	) {
		this.user = user;
		this.familyId = familyId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	public static AuthSession create(
			User user,
			UUID familyId,
			String tokenHash,
			OffsetDateTime expiresAt,
			OffsetDateTime createdAt
	) {
		return new AuthSession(user, familyId, tokenHash, expiresAt, createdAt);
	}

	public void rotateTo(Long replacementSessionId, OffsetDateTime rotatedAt) {
		this.lastUsedAt = rotatedAt;
		this.rotatedAt = rotatedAt;
		this.replacedBySessionId = replacementSessionId;
	}

	public void markReuseDetected(OffsetDateTime detectedAt) {
		this.reuseDetectedAt = detectedAt;
	}

	public boolean isRotated() {
		return rotatedAt != null;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(OffsetDateTime now) {
		return !expiresAt.isAfter(now);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public UUID getFamilyId() {
		return familyId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getLastUsedAt() {
		return lastUsedAt;
	}

	public OffsetDateTime getRotatedAt() {
		return rotatedAt;
	}

	public OffsetDateTime getRevokedAt() {
		return revokedAt;
	}

	public OffsetDateTime getReuseDetectedAt() {
		return reuseDetectedAt;
	}

	public Long getReplacedBySessionId() {
		return replacedBySessionId;
	}
}
