package com.sellerradar.plan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan {
	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "code", nullable = false, length = 20)
	private Plan code;

	@Column(name = "display_name", nullable = false, length = 50)
	private String displayName;

	@Column(name = "keyword_limit", nullable = false)
	private int keywordLimit;

	@Column(name = "csv_row_limit", nullable = false)
	private int csvRowLimit;

	@Column(name = "candidate_limit", nullable = false)
	private int candidateLimit;

	protected SubscriptionPlan() {
	}

	private SubscriptionPlan(Plan code, String displayName) {
		this.code = code;
		this.displayName = displayName;
		this.keywordLimit = code.keywordLimit();
		this.csvRowLimit = code.csvRowLimit();
		this.candidateLimit = code.candidateLimit();
	}

	public static SubscriptionPlan from(Plan plan) {
		return new SubscriptionPlan(plan, plan.name());
	}

	public Plan getCode() {
		return code;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getKeywordLimit() {
		return keywordLimit;
	}

	public int getCsvRowLimit() {
		return csvRowLimit;
	}

	public int getCandidateLimit() {
		return candidateLimit;
	}
}
