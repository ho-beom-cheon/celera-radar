package com.sellerradar.auth.securityevent;

import com.sellerradar.auth.ratelimit.AuthRateLimitAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "auth_security_events")
public class AuthSecurityEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private AuthSecurityEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuthRateLimitAction action;

	@Column(name = "subject_hash", length = 64)
	private String subjectHash;

	@Column(name = "network_hash", length = 64)
	private String networkHash;

	@Column(name = "retry_after_seconds")
	private Integer retryAfterSeconds;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private OffsetDateTime occurredAt;

	protected AuthSecurityEvent() {
	}

	private AuthSecurityEvent(
			AuthRateLimitAction action,
			String subjectHash,
			String networkHash,
			int retryAfterSeconds,
			OffsetDateTime occurredAt
	) {
		this.eventType = AuthSecurityEventType.RATE_LIMITED;
		this.action = action;
		this.subjectHash = subjectHash;
		this.networkHash = networkHash;
		this.retryAfterSeconds = retryAfterSeconds;
		this.occurredAt = occurredAt;
	}

	public static AuthSecurityEvent rateLimited(
			AuthRateLimitAction action,
			String subjectHash,
			String networkHash,
			int retryAfterSeconds,
			OffsetDateTime occurredAt
	) {
		return new AuthSecurityEvent(action, subjectHash, networkHash, retryAfterSeconds, occurredAt);
	}

	public Long getId() {
		return id;
	}

	public AuthSecurityEventType getEventType() {
		return eventType;
	}

	public AuthRateLimitAction getAction() {
		return action;
	}

	public String getSubjectHash() {
		return subjectHash;
	}

	public String getNetworkHash() {
		return networkHash;
	}

	public Integer getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

	public OffsetDateTime getOccurredAt() {
		return occurredAt;
	}
}
