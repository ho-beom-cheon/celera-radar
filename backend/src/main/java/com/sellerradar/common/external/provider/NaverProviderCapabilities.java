package com.sellerradar.common.external.provider;

import com.sellerradar.common.external.config.NaverApiHubProperties;
import com.sellerradar.common.external.config.NaverApiProperties;
import com.sellerradar.common.external.config.NaverProviderProperties;
import java.util.EnumSet;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NaverProviderCapabilities {
	private final NaverProviderProperties providerProperties;
	private final NaverApiProperties legacyProperties;
	private final NaverApiHubProperties hubProperties;

	public NaverProviderCapabilities(
			NaverProviderProperties providerProperties,
			NaverApiProperties legacyProperties,
			NaverApiHubProperties hubProperties
	) {
		this.providerProperties = providerProperties;
		this.legacyProperties = legacyProperties;
		this.hubProperties = hubProperties;
	}

	public ExternalProviderDescriptor descriptor() {
		EnumSet<ExternalCapability> capabilities = EnumSet.noneOf(ExternalCapability.class);
		switch (providerProperties.mode()) {
			case LEGACY -> {
				if (hasLegacyCredentials()) {
					capabilities.add(ExternalCapability.SHOPPING_SEARCH);
					capabilities.add(ExternalCapability.SHOPPING_INSIGHT);
				}
			}
			case HUB -> {
				if (hubProperties.hasCredentials() && hubProperties.hasShoppingSearchEndpoint()) {
					capabilities.add(ExternalCapability.SHOPPING_SEARCH);
				}
				if (hubProperties.hasCredentials() && hubProperties.hasShoppingInsightEndpoint()) {
					capabilities.add(ExternalCapability.SHOPPING_INSIGHT);
				}
			}
			case DISABLED -> {
				// Explicitly no external capabilities.
			}
		}
		return new ExternalProviderDescriptor(providerProperties.mode(), capabilities);
	}

	private boolean hasLegacyCredentials() {
		return StringUtils.hasText(legacyProperties.clientId())
				&& StringUtils.hasText(legacyProperties.clientSecret());
	}
}
