package com.sellerradar.smartstore.client;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import java.util.List;

public interface SmartStoreClient {
	SmartStoreConnectionProfile fetchConnectionProfile(SmartStoreConnection connection);

	List<SmartStoreProductSyncItem> fetchProducts(SmartStoreConnection connection);
}
