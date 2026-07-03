package com.sellerradar.plan.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_subscription")
public class UserSubscription {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_code", nullable = false)
	private SubscriptionPlan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserSubscriptionStatus status;

	@Column(name = "started_at", nullable = false)
	private OffsetDateTime startedAt;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected UserSubscription() {
	}

	private UserSubscription(User user, SubscriptionPlan plan, UserSubscriptionStatus status, OffsetDateTime startedAt) {
		this.user = user;
		this.plan = plan;
		this.status = status;
		this.startedAt = startedAt;
	}

	public static UserSubscription free(User user, SubscriptionPlan plan) {
		if (plan.getCode() != Plan.FREE) {
			throw new IllegalArgumentException("초기 구독 플랜은 FREE여야 합니다.");
		}
		return new UserSubscription(user, plan, UserSubscriptionStatus.ACTIVE, OffsetDateTime.now());
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

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public SubscriptionPlan getPlan() {
		return plan;
	}

	public UserSubscriptionStatus getStatus() {
		return status;
	}

	public OffsetDateTime getStartedAt() {
		return startedAt;
	}

	public OffsetDateTime getExpiresAt() {
		return expiresAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
