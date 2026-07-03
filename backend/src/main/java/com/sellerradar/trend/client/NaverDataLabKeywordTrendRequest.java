package com.sellerradar.trend.client;

import java.time.LocalDate;
import org.springframework.util.StringUtils;

public record NaverDataLabKeywordTrendRequest(
		LocalDate startDate,
		LocalDate endDate,
		NaverDataLabTimeUnit timeUnit,
		String category,
		String keyword
) {
	private static final LocalDate MIN_START_DATE = LocalDate.of(2017, 8, 1);

	public NaverDataLabKeywordTrendRequest {
		if (startDate == null || endDate == null) {
			throw new IllegalArgumentException("조회 시작일과 종료일은 필수입니다.");
		}
		if (startDate.isBefore(MIN_START_DATE)) {
			throw new IllegalArgumentException("startDate는 2017-08-01 이후여야 합니다.");
		}
		if (startDate.isAfter(endDate)) {
			throw new IllegalArgumentException("startDate는 endDate보다 늦을 수 없습니다.");
		}
		if (timeUnit == null) {
			timeUnit = NaverDataLabTimeUnit.DATE;
		}
		if (!StringUtils.hasText(category)) {
			throw new IllegalArgumentException("카테고리 코드는 필수입니다.");
		}
		if (!StringUtils.hasText(keyword)) {
			throw new IllegalArgumentException("키워드는 필수입니다.");
		}
	}
}
