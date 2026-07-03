package com.sellerradar.trend.client;

import java.util.List;
import org.springframework.util.StringUtils;

public record NaverDataLabKeywordGroup(
		String name,
		List<String> param
) {
	public NaverDataLabKeywordGroup {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("키워드 그룹 이름은 필수입니다.");
		}
		if (param == null || param.size() != 1 || !StringUtils.hasText(param.getFirst())) {
			throw new IllegalArgumentException("키워드는 1개만 지정해야 합니다.");
		}
	}
}
