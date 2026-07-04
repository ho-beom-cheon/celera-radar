package com.sellerradar.smartstore.settlement.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SmartStoreSettlementSnapshotItem(
		LocalDate settlementDate,
		String productOrderNo,
		BigDecimal settlementAmount,
		String rawPayload
) {
}
