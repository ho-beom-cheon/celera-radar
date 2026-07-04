package com.sellerradar.smartstore.settlement.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SmartStoreCommissionSnapshotItem(
		LocalDate baseDate,
		String productOrderNo,
		String commissionType,
		BigDecimal commissionAmount,
		String rawPayload
) {
}
