package com.sellerradar.alert.domain;

import com.sellerradar.candidate.domain.ProductCandidate;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;

@Entity
@Table(
		name = "alert",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_alert_rule_candidate_type",
				columnNames = {"rule_id", "candidate_id", "type"}
		)
)
public class Alert {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rule_id", nullable = false)
	private AlertRule rule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "candidate_id", nullable = false)
	private ProductCandidate candidate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AlertType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AlertStatus status;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "read_at")
	private OffsetDateTime readAt;

	protected Alert() {
	}

	private Alert(
			User user,
			AlertRule rule,
			ProductCandidate candidate,
			AlertType type,
			String title,
			String message
	) {
		this.user = user;
		this.rule = rule;
		this.candidate = candidate;
		this.type = type;
		this.status = AlertStatus.UNREAD;
		this.title = title;
		this.message = message;
	}

	public static Alert candidateScore(AlertRule rule, ProductCandidate candidate) {
		String title = "검토 후보 조건에 맞는 상품이 있습니다.";
		String message = candidate.getName() + " 후보가 알림 조건을 충족했습니다. 점수와 마진을 확인하세요.";
		return new Alert(rule.getUser(), rule, candidate, AlertType.CANDIDATE_SCORE, title, message);
	}

	public void markRead(OffsetDateTime readAt) {
		this.status = AlertStatus.READ;
		this.readAt = readAt;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public AlertRule getRule() {
		return rule;
	}

	public ProductCandidate getCandidate() {
		return candidate;
	}

	public AlertType getType() {
		return type;
	}

	public AlertStatus getStatus() {
		return status;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getReadAt() {
		return readAt;
	}
}
