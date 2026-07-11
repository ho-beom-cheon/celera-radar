package com.sellerradar.shopping.provider;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.provider.ExternalCapability;
import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import com.sellerradar.common.external.provider.NaverProviderCapabilities;
import com.sellerradar.shopping.client.NaverApiHubShoppingClient;
import com.sellerradar.shopping.client.NaverShoppingClient;
import com.sellerradar.shopping.client.NaverShoppingSearchRequest;
import com.sellerradar.shopping.client.NaverShoppingSearchResponse;
import com.sellerradar.shopping.port.ShoppingSearchProvider;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredShoppingSearchProvider implements ShoppingSearchProvider {
	private final NaverProviderCapabilities capabilities;
	private final NaverShoppingClient legacyClient;
	private final NaverApiHubShoppingClient hubClient;

	public ConfiguredShoppingSearchProvider(
			NaverProviderCapabilities capabilities,
			NaverShoppingClient legacyClient,
			NaverApiHubShoppingClient hubClient
	) {
		this.capabilities = capabilities;
		this.legacyClient = legacyClient;
		this.hubClient = hubClient;
	}

	@Override
	public ExternalProviderDescriptor descriptor() {
		return capabilities.descriptor();
	}

	@Override
	public NaverShoppingSearchResponse search(NaverShoppingSearchRequest request) {
		ExternalProviderDescriptor descriptor = descriptor();
		if (!descriptor.supports(ExternalCapability.SHOPPING_SEARCH)) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
		}
		return switch (descriptor.mode()) {
			case LEGACY -> legacyClient.search(request);
			case HUB -> hubClient.search(request);
			case DISABLED -> throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
		};
	}
}
