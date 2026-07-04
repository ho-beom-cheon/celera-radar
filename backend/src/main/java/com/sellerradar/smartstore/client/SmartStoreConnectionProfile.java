package com.sellerradar.smartstore.client;

import com.sellerradar.smartstore.domain.SmartStoreConnectionStatus;

public record SmartStoreConnectionProfile(
		Long connectionId,
		String storeId,
		String storeName,
		String sellerId,
		SmartStoreConnectionStatus status
) {
}
