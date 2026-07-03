package com.sellerradar.smartstore.client;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockSmartStoreClient implements SmartStoreClient {
	private final List<SmartStoreProductSyncItem> seedItems;

	public MockSmartStoreClient() {
		this(List.of());
	}

	public MockSmartStoreClient(List<SmartStoreProductSyncItem> seedItems) {
		this.seedItems = seedItems;
	}

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

	@Override
	public List<SmartStoreProductSyncItem> fetchProducts(SmartStoreConnection connection) {
		if (!seedItems.isEmpty()) {
			return seedItems;
		}
		String productId = "mock-product-" + connection.getId();
		return List.of(new SmartStoreProductSyncItem(
				productId,
				null,
				"Mock SmartStore Product",
				new BigDecimal("12900"),
				"SALE",
				null,
				null,
				"{\"mock\":true}"
		));
	}
}
