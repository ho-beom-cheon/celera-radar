package com.sellerradar.smartstore.client;

import com.sellerradar.smartstore.domain.SmartStoreConnection;

public class MockSmartStoreClient implements SmartStoreClient {
	@Override
	public SmartStoreConnectionProfile fetchConnectionProfile(SmartStoreConnection connection) {
		return new SmartStoreConnectionProfile(
				connection.getId(),
				connection.getStoreId(),
				connection.getStoreName(),
				connection.getSellerId(),
				connection.getConnectionStatus()
		);
	}
}
