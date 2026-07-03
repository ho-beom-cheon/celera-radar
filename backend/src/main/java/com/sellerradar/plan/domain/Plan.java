package com.sellerradar.plan.domain;

public enum Plan {
	FREE(3, 100, 10),
	BASIC(30, 3_000, 100),
	PRO(100, 20_000, 500);

	private final int keywordLimit;
	private final int csvRowLimit;
	private final int candidateLimit;

	Plan(int keywordLimit, int csvRowLimit, int candidateLimit) {
		this.keywordLimit = keywordLimit;
		this.csvRowLimit = csvRowLimit;
		this.candidateLimit = candidateLimit;
	}

	public int keywordLimit() {
		return keywordLimit;
	}

	public int csvRowLimit() {
		return csvRowLimit;
	}

	public int candidateLimit() {
		return candidateLimit;
	}
}
