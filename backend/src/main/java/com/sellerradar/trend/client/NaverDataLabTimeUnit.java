package com.sellerradar.trend.client;

public enum NaverDataLabTimeUnit {
	DATE("date"),
	WEEK("week"),
	MONTH("month");

	private final String value;

	NaverDataLabTimeUnit(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
