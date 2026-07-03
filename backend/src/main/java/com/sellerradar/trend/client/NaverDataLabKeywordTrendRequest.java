package com.sellerradar.trend.client;

import java.time.LocalDate;
import java.util.List;
import org.springframework.util.StringUtils;

public record NaverDataLabKeywordTrendRequest(
		LocalDate startDate,
		LocalDate endDate,
		NaverDataLabTimeUnit timeUnit,
		String category,
		List<NaverDataLabKeywordGroup> keywordGroups
) {
	private static final LocalDate MIN_START_DATE = LocalDate.of(2017, 8, 1);
	public static final int MAX_KEYWORD_GROUPS = 5;

	public NaverDataLabKeywordTrendRequest(
			LocalDate startDate,
			LocalDate endDate,
			NaverDataLabTimeUnit timeUnit,
			String category,
			String keyword
	) {
		this(
				startDate,
				endDate,
				timeUnit,
				category,
				List.of(new NaverDataLabKeywordGroup(keyword, List.of(keyword)))
		);
	}

	public NaverDataLabKeywordTrendRequest {
		if (startDate == null || endDate == null) {
			throw new IllegalArgumentException("startDate and endDate are required.");
		}
		if (startDate.isBefore(MIN_START_DATE)) {
			throw new IllegalArgumentException("startDate must be on or after 2017-08-01.");
		}
		if (startDate.isAfter(endDate)) {
			throw new IllegalArgumentException("startDate must not be after endDate.");
		}
		if (timeUnit == null) {
			timeUnit = NaverDataLabTimeUnit.DATE;
		}
		if (!StringUtils.hasText(category)) {
			throw new IllegalArgumentException("category is required.");
		}
		if (keywordGroups == null || keywordGroups.isEmpty()) {
			throw new IllegalArgumentException("At least one keyword group is required.");
		}
		if (keywordGroups.size() > MAX_KEYWORD_GROUPS) {
			throw new IllegalArgumentException("DataLab keyword groups must be 5 or fewer.");
		}
		keywordGroups = List.copyOf(keywordGroups);
	}

	public String keyword() {
		return keywordGroups.getFirst().param().getFirst();
	}
}
