package com.sellerradar.common.external.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "seller-radar.naver-api-hub")
public record NaverApiHubProperties(
		String clientId,
		String clientSecret,
		String shoppingSearchEndpoint,
		String shoppingInsightEndpoint
) {
	public boolean hasCredentials() {
		return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
	}

	public boolean hasShoppingSearchEndpoint() {
		return isHttpEndpoint(shoppingSearchEndpoint);
	}

	public boolean hasShoppingInsightEndpoint() {
		return isHttpEndpoint(shoppingInsightEndpoint);
	}

	private boolean isHttpEndpoint(String value) {
		if (!StringUtils.hasText(value)) {
			return false;
		}
		try {
			URI uri = new URI(value.strip());
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			return (scheme.equals("http") || scheme.equals("https"))
					&& uri.getHost() != null
					&& uri.getUserInfo() == null;
		} catch (URISyntaxException exception) {
			return false;
		}
	}
}
