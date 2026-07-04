package com.sellerradar.smartstore.client;

import com.sellerradar.smartstore.domain.SmartStoreConnection;

public interface SmartStoreClient {
	SmartStoreConnectionProfile fetchConnectionProfile(SmartStoreConnection connection);
}
