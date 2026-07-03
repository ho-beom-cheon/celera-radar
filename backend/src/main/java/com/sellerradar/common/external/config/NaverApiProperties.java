package com.sellerradar.common.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seller-radar.naver")
public record NaverApiProperties(
		String clientId,
		String clientSecret,
		int datalabDailyQuota
) {
}
