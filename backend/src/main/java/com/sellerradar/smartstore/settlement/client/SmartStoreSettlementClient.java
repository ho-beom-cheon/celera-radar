package com.sellerradar.smartstore.settlement.client;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import java.time.LocalDate;
import java.util.List;

public interface SmartStoreSettlementClient {
	List<SmartStoreOrderSnapshotItem> fetchOrders(SmartStoreConnection connection, LocalDate from, LocalDate to);

	List<SmartStoreSettlementSnapshotItem> fetchSettlements(SmartStoreConnection connection, LocalDate from, LocalDate to);

	List<SmartStoreCommissionSnapshotItem> fetchCommissions(SmartStoreConnection connection, LocalDate from, LocalDate to);
}
