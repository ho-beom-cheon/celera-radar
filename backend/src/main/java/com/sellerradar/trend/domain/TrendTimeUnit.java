package com.sellerradar.trend.domain;

import com.sellerradar.trend.client.NaverDataLabTimeUnit;

public enum TrendTimeUnit {
	DATE,
	WEEK,
	MONTH;

	public static TrendTimeUnit from(NaverDataLabTimeUnit timeUnit) {
		return TrendTimeUnit.valueOf(timeUnit.name());
	}
}
