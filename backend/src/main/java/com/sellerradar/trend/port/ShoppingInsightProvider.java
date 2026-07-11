package com.sellerradar.trend.port;

import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendRequest;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendResponse;

public interface ShoppingInsightProvider {
	ExternalProviderDescriptor descriptor();

	NaverDataLabKeywordTrendResponse searchKeywordTrend(NaverDataLabKeywordTrendRequest request);
}
