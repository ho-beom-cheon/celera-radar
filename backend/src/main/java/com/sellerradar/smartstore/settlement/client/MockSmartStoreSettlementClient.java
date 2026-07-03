package com.sellerradar.smartstore.settlement.client;

import com.sellerradar.smartstore.domain.SmartStoreConnection;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockSmartStoreSettlementClient implements SmartStoreSettlementClient {
	private final List<SmartStoreOrderSnapshotItem> orders;
	private final List<SmartStoreSettlementSnapshotItem> settlements;
	private final List<SmartStoreCommissionSnapshotItem> commissions;

	public MockSmartStoreSettlementClient() {
		this(List.of(), List.of(), List.of());
	}

	public MockSmartStoreSettlementClient(
			List<SmartStoreOrderSnapshotItem> orders,
			List<SmartStoreSettlementSnapshotItem> settlements,
			List<SmartStoreCommissionSnapshotItem> commissions
	) {
		this.orders = orders;
		this.settlements = settlements;
		this.commissions = commissions;
	}

	@Override
	public List<SmartStoreOrderSnapshotItem> fetchOrders(
			SmartStoreConnection connection,
			LocalDate from,
			LocalDate to
	) {
		return orders;
	}

	@Override
	public List<SmartStoreSettlementSnapshotItem> fetchSettlements(
			SmartStoreConnection connection,
			LocalDate from,
			LocalDate to
	) {
		return settlements;
	}

	@Override
	public List<SmartStoreCommissionSnapshotItem> fetchCommissions(
			SmartStoreConnection connection,
			LocalDate from,
			LocalDate to
	) {
		return commissions;
	}
}
