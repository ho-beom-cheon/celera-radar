package com.sellerradar.shopping.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.provider.ExternalCapability;
import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import com.sellerradar.common.external.provider.ExternalProviderMode;
import com.sellerradar.common.external.provider.NaverProviderCapabilities;
import com.sellerradar.shopping.client.NaverApiHubShoppingClient;
import com.sellerradar.shopping.client.NaverShoppingClient;
import com.sellerradar.shopping.client.NaverShoppingSearchRequest;
import com.sellerradar.shopping.client.NaverShoppingSort;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfiguredShoppingSearchProviderTest {
	private final NaverProviderCapabilities capabilities = mock(NaverProviderCapabilities.class);
	private final NaverShoppingClient legacy = mock(NaverShoppingClient.class);
	private final NaverApiHubShoppingClient hub = mock(NaverApiHubShoppingClient.class);
	private final ConfiguredShoppingSearchProvider provider = new ConfiguredShoppingSearchProvider(
			capabilities,
			legacy,
			hub
	);
	private final NaverShoppingSearchRequest request = new NaverShoppingSearchRequest(
			"desk",
			10,
			1,
			NaverShoppingSort.SIM,
			""
	);

	@Test
	void routesLegacyModeOnlyToLegacyAdapter() {
		when(capabilities.descriptor()).thenReturn(descriptor(ExternalProviderMode.LEGACY, true));

		provider.search(request);

		verify(legacy).search(request);
		verify(hub, never()).search(request);
	}

	@Test
	void routesHubModeOnlyToHubAdapter() {
		when(capabilities.descriptor()).thenReturn(descriptor(ExternalProviderMode.HUB, true));

		provider.search(request);

		verify(hub).search(request);
		verify(legacy, never()).search(request);
	}

	@Test
	void disabledModeNeverCallsNetworkAdapters() {
		when(capabilities.descriptor()).thenReturn(descriptor(ExternalProviderMode.DISABLED, false));

		assertThatThrownBy(() -> provider.search(request))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						org.assertj.core.api.Assertions.assertThat(exception.errorCode())
								.isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE));
		verify(legacy, never()).search(request);
		verify(hub, never()).search(request);
	}

	private ExternalProviderDescriptor descriptor(ExternalProviderMode mode, boolean enabled) {
		return new ExternalProviderDescriptor(
				mode,
				enabled ? Set.of(ExternalCapability.SHOPPING_SEARCH) : Set.of()
		);
	}
}
