package com.sellerradar.user.domain;

import com.sellerradar.plan.domain.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, updatable = false)
	private UUID publicId;

	@Column(length = 255)
	private String email;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "plan_code", nullable = false, length = 30)
	private Plan planCode;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected User() {
	}

	private User(String email, String passwordHash, String displayName, UserRole role, Plan planCode) {
		this.publicId = UUID.randomUUID();
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.role = role;
		this.planCode = planCode;
		this.active = true;
	}

	public static User create(String email, String passwordHash) {
		return new User(email, passwordHash, defaultDisplayName(email), UserRole.USER, Plan.FREE);
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		if (this.publicId == null) {
			this.publicId = UUID.randomUUID();
		}
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public void delete() {
		this.active = false;
		this.deletedAt = OffsetDateTime.now();
	}

	private static String defaultDisplayName(String email) {
		if (email == null || email.isBlank()) {
			return null;
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 0) {
			return email;
		}
		return email.substring(0, atIndex);
	}

	public Long getId() {
		return id;
	}

	public UUID getPublicId() {
		return publicId;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public Plan getPlanCode() {
		return planCode;
	}

	public boolean isActive() {
		return active;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}
}
