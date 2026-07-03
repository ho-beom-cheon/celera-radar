package com.sellerradar.smartstore.dto;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import com.sellerradar.smartstore.domain.SmartStoreConnectionStatus;
import java.time.OffsetDateTime;

public record SmartStoreConnectionStatusResponse(
		Long connectionId,
		String storeName,
		String storeId,
		String sellerId,
		SmartStoreConnectionStatus status,
		OffsetDateTime tokenExpiresAt,
		OffsetDateTime lastSyncedAt
) {
	public static SmartStoreConnectionStatusResponse disconnected() {
		return new SmartStoreConnectionStatusResponse(
				null,
				null,
				null,
				null,
				SmartStoreConnectionStatus.DISCONNECTED,
				null,
				null
		);
	}

	public static SmartStoreConnectionStatusResponse from(SmartStoreConnection connection) {
		return new SmartStoreConnectionStatusResponse(
				connection.getId(),
				connection.getStoreName(),
				connection.getStoreId(),
				connection.getSellerId(),
				connection.getConnectionStatus(),
				connection.getTokenExpiresAt(),
				connection.getLastSyncedAt()
		);
	}
}
