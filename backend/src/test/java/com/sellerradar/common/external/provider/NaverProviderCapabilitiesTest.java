package com.sellerradar.common.external.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.sellerradar.common.external.config.NaverApiHubProperties;
import com.sellerradar.common.external.config.NaverApiProperties;
import com.sellerradar.common.external.config.NaverProviderProperties;
import org.junit.jupiter.api.Test;

class NaverProviderCapabilitiesTest {
	@Test
	void legacyRequiresOnlyLegacyCredentials() {
		ExternalProviderDescriptor descriptor = capabilities(
				ExternalProviderMode.LEGACY,
				new NaverApiProperties("legacy-id", "legacy-secret", 1000),
				emptyHub()
		).descriptor();

		assertThat(descriptor.capabilities())
				.containsExactlyInAnyOrder(ExternalCapability.SHOPPING_SEARCH, ExternalCapability.SHOPPING_INSIGHT);
	}

	@Test
	void hubEnablesOnlyCapabilitiesWithExplicitEndpointAndHubCredentials() {
		ExternalProviderDescriptor descriptor = capabilities(
				ExternalProviderMode.HUB,
				new NaverApiProperties("legacy-id", "legacy-secret", 1000),
				new NaverApiHubProperties("hub-id", "hub-secret", "", "https://hub.example.com/insight")
		).descriptor();

		assertThat(descriptor.capabilities()).containsExactly(ExternalCapability.SHOPPING_INSIGHT);
	}

	@Test
	void disabledNeverExposesCapabilitiesEvenWhenCredentialsExist() {
		ExternalProviderDescriptor descriptor = capabilities(
				ExternalProviderMode.DISABLED,
				new NaverApiProperties("legacy-id", "legacy-secret", 1000),
				new NaverApiHubProperties(
						"hub-id",
						"hub-secret",
						"https://hub.example.com/search",
						"https://hub.example.com/insight"
				)
		).descriptor();

		assertThat(descriptor.capabilities()).isEmpty();
	}

	@Test
	void hubRejectsNonHttpEndpointBeforeAnyAdapterCall() {
		ExternalProviderDescriptor descriptor = capabilities(
				ExternalProviderMode.HUB,
				new NaverApiProperties("", "", 1000),
				new NaverApiHubProperties("hub-id", "hub-secret", "file:///tmp/search", "javascript:alert(1)")
		).descriptor();

		assertThat(descriptor.capabilities()).isEmpty();
	}

	private NaverProviderCapabilities capabilities(
			ExternalProviderMode mode,
			NaverApiProperties legacy,
			NaverApiHubProperties hub
	) {
		return new NaverProviderCapabilities(new NaverProviderProperties(mode), legacy, hub);
	}

	private NaverApiHubProperties emptyHub() {
		return new NaverApiHubProperties("", "", "", "");
	}
}
