package com.sellerradar.shopping.client;

import org.springframework.util.StringUtils;

public record NaverShoppingSearchRequest(
		String query,
		int display,
		int start,
		NaverShoppingSort sort,
		String exclude
) {
	private static final int MIN_DISPLAY = 1;
	private static final int MAX_DISPLAY = 100;
	private static final int MIN_START = 1;
	private static final int MAX_START = 1000;

	public NaverShoppingSearchRequest {
		if (!StringUtils.hasText(query)) {
			throw new IllegalArgumentException("검색어는 필수입니다.");
		}
		if (display < MIN_DISPLAY || display > MAX_DISPLAY) {
			throw new IllegalArgumentException("display는 1 이상 100 이하입니다.");
		}
		if (start < MIN_START || start > MAX_START) {
			throw new IllegalArgumentException("start는 1 이상 1000 이하입니다.");
		}
		if (sort == null) {
			sort = NaverShoppingSort.SIM;
		}
	}
}
