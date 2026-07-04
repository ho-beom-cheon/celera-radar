package com.sellerradar.smartstore.settlement.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SmartStoreOrderSnapshotItem(
		String orderNo,
		String productOrderNo,
		OffsetDateTime orderDate,
		BigDecimal paymentAmount,
		int quantity,
		String orderStatus,
		String rawPayload
) {
}
