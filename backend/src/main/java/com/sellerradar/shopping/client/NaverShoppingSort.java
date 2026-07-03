package com.sellerradar.shopping.client;

public enum NaverShoppingSort {
	SIM("sim"),
	DATE("date"),
	ASC("asc"),
	DSC("dsc");

	private final String value;

	NaverShoppingSort(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}
}
