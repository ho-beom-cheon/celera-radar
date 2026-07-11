package com.sellerradar.trend.provider;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.provider.ExternalCapability;
import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import com.sellerradar.common.external.provider.NaverProviderCapabilities;
import com.sellerradar.trend.client.NaverApiHubDataLabClient;
import com.sellerradar.trend.client.NaverDataLabClient;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendRequest;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendResponse;
import com.sellerradar.trend.port.ShoppingInsightProvider;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredShoppingInsightProvider implements ShoppingInsightProvider {
	private final NaverProviderCapabilities capabilities;
	private final NaverDataLabClient legacyClient;
	private final NaverApiHubDataLabClient hubClient;

	public ConfiguredShoppingInsightProvider(
			NaverProviderCapabilities capabilities,
			NaverDataLabClient legacyClient,
			NaverApiHubDataLabClient hubClient
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
	public NaverDataLabKeywordTrendResponse searchKeywordTrend(NaverDataLabKeywordTrendRequest request) {
		ExternalProviderDescriptor descriptor = descriptor();
		if (!descriptor.supports(ExternalCapability.SHOPPING_INSIGHT)) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
		}
		return switch (descriptor.mode()) {
			case LEGACY -> legacyClient.searchKeywordTrend(request);
			case HUB -> hubClient.searchKeywordTrend(request);
			case DISABLED -> throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
		};
	}
}
