package com.sellerradar.common.external.config;

import com.sellerradar.common.external.provider.ExternalProviderMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seller-radar.external.naver")
public record NaverProviderProperties(
		ExternalProviderMode mode
) {
}
