package com.sellerradar.shopping.port;

import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import com.sellerradar.shopping.client.NaverShoppingSearchRequest;
import com.sellerradar.shopping.client.NaverShoppingSearchResponse;

public interface ShoppingSearchProvider {
	ExternalProviderDescriptor descriptor();

	NaverShoppingSearchResponse search(NaverShoppingSearchRequest request);
}
